(ns beetleman.nipper
  (:refer-clojure :exclude [load])
  (:require [taoensso.nippy :as nippy]
            [clojure.java.io :as io])
  (:import [java.io DataInputStream]))


(defn- file-name [idx]
  (str idx ".npy"))


(defn- create-index-data
  ([data]
   (create-index-data data 10))
  ([data part-size]
   (let [parts (->> data
                    keys
                    (partition-all part-size))]
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


(defn dump!
  ([path data]
   (dump! path data (calc-part-size data)))
  ([path data part-size]
   (.mkdir (java.io.File. path))
   (let [index-data (create-index-data data part-size)
         index      (keys index-data)]
     (save-index! path index)
     (save-meta! path data)
     (doseq [[fname ks] index-data]
       (fast-freeze-to-file (str path "/" fname)
                            (select-keys data ks))))))


(defn- fast-thaw-from-file
  ([file]
   (let [file (io/file file)
         ba   (byte-array (.length file))]
     (with-open [in (DataInputStream. (io/input-stream file))]
       (.readFully in ba))
     (nippy/fast-thaw ba))))


(defn load! [path]
  (load-meta! path
              (reduce
               (fn [acc fname]
                 (merge acc (fast-thaw-from-file (str path "/" fname))))
               {}
               (load-index! path))))
