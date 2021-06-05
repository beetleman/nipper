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
  ([data]
   (calc-part-size data 5))
  ([data percentage]
   (let [size      (count data)
         part-size (int (* size (/ percentage 100)))]
     (if (zero? part-size)
       size
       part-size))))


(defn- fast-freeze-to-file
  ([file x]
   (let [^bytes ba (nippy/fast-freeze x)]
     (with-open [out (io/output-stream (io/file file))]
       (.write out ba))
     file)))


(defn- default-progress-cb [kind p]
  (log/info kind ": " p "%"))


(defn calc-progress [idx max-idx]
  (if (zero? max-idx)
    0
    (int (* (/ idx max-idx) 100.0))))


(defn dump!
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
  ([file]
   (let [file (io/file file)
         ba   (byte-array (.length file))]
     (with-open [in (DataInputStream. (io/input-stream file))]
       (.readFully in ba))
     (nippy/fast-thaw ba))))


(defn load!
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
