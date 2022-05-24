(ns beetleman.nipper
  (:refer-clojure :exclude [load])
  (:require [taoensso.nippy :as nippy]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import [java.io DataInputStream]))


(defn- file-name [idx]
  (str idx ".npy"))


(defn- remove-not-exist-keys-from-part [data part]
  (filter #(contains? data %)
          part))


(defn- create-index-data
  ([data]
   (create-index-data data 10 []))
  ([data part-size force-parts]
   (let [force-parts (into []
                           (comp (map #(remove-not-exist-keys-from-part data %))
                                 (remove empty?))
                           force-parts)
         keys        (keys (apply dissoc
                                  data
                                  (mapcat identity force-parts)))
         parts       (into force-parts
                           (partition-all part-size keys))]
     (into {}
           (map-indexed (fn [idx part]
                          [(file-name idx) part])
                        parts)))))


(defn- save-index! [path index]
  (nippy/freeze-to-file (str path "/" "index.npy") index))


(defn- load-index! [path]
  (nippy/thaw-from-file (str path "/" "index.npy")))


(defn- save-meta! [path data]
  (nippy/freeze-to-file (str path "/" "meta.npy")
                        (meta data)))


(defn- load-meta! [path data]
  (with-meta data
             (nippy/thaw-from-file (str path "/" "meta.npy"))))


(defn calc-part-size
  "calculate part size based on percentage
  - `data` - HashMap with data
  - `percentage` - percentage for with size pill be calculated, default `5`"
  ([data]
   (calc-part-size data 5))
  ([data percentage]
   (let [size      (count data)
         part-size (int (* size (/ percentage 100)))]
     (if (zero? part-size)
       size
       part-size))))


(defn- fast-freeze-to-file
  "Faster (in this usecase) version of nippy/fast-freeze
  - `file` - path to file
  - `data` - data to save"
  [file data]
  (let [^bytes ba (nippy/fast-freeze data)]
    (with-open [out (io/output-stream (io/file file))]
      (.write out ba))
    file))


(defn- default-progress-cb [kind p]
  (log/info kind ": " p "%"))


(defn- calc-progress
  "Calc progress, returns percentage"
  [idx max-idx]
  (if (zero? max-idx)
    0
    (int (* (/ idx max-idx) 100.0))))


(defn dump!
  "Save HashMap do filesystem
  - `path` - path where HashMap will be saved and later can be loaded via [[load!]]
  - `data` - HashMap to persist
  additional options map with keys:
  - `part-size` - size of data chunk saved to disk, number of key saved to single file in `path`,
    defaults `5%`. [[calc-part-size]] can be used to calculate size using percent values
  - `force-parts` - parts saved in seperate files, ignore `part-size`, vector of vectors:
     `[[:key-1] [:key-2 :key-3]]` - `:key-1` will be saved in one file and keys `:key-2` and `:key-3` in another
     rest of keys will be saved in parts devined by `part-size`
  - `progress-cb` - function called on load progress, eg. `(fn [percentage] (println percentage \"%\"))`"
  ([path data]
   (dump! path data {}))
  ([path data
    {:keys [part-size
            force-parts
            progress-cb]
     :or   {progress-cb (partial default-progress-cb "DUMP")
            force-parts []}}]
   (.mkdir (java.io.File. path))
   (let [part-size  (or part-size (calc-part-size data))
         index-data (create-index-data data part-size force-parts)
         index      (keys index-data)
         max-idx    (dec (count index))]
     (doseq [[idx [fname ks]] (map-indexed vector index-data)]
       (progress-cb (calc-progress idx max-idx))
       (fast-freeze-to-file (str path "/" fname)
                            (select-keys data ks)))
     (save-meta! path data)
     (save-index! path index)
     path)))


(defn- fast-thaw-from-file
  "Faster (in this usecase) version of `nippy/fast-thaw`"
  ([file]
   (let [file (io/file file)
         ba   (byte-array (.length file))]
     (with-open [in (DataInputStream. (io/input-stream file))]
       (.readFully in ba))
     (nippy/fast-thaw ba))))


(defn load!
  "Load HashMap from filesystem
  - `path` - path to directory where data was saved via [[dump!]]
  additional options map with keys:
  - `progress-cb` - function called on load progress, eg. `(fn [percentage] (println percentage \"%\"))`"
  ([path]
   (load! path {}))
  ([path
    {:keys [progress-cb]
     :or   {progress-cb (partial default-progress-cb "LOAD")}}]
   (let [index   (load-index! path)
         max-idx (dec (count index))]
     (load-meta! path
                 (persistent!
                  (reduce
                   (fn [acc [idx fname]]
                     (progress-cb (calc-progress idx max-idx))
                     (conj! acc (fast-thaw-from-file (str path "/" fname))))
                   (transient {})
                   (map-indexed vector index)))))))
