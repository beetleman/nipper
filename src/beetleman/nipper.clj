(ns beetleman.nipper
  (:refer-clojure :exclude [load])
  (:require [taoensso.nippy :as nippy]))


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


(defn calc-part-size
  ([data]
   (calc-part-size data 5))
  ([data percentage]
   (let [size      (count data)
         part-size (int (* size (/ percentage 100)))]
     (if (zero? part-size)
       size
       part-size))))


(defn dump
  ([path data]
   (dump path data (calc-part-size data)))
  ([path data part-size]
   (.mkdir (java.io.File. path))
   (let [index-data (create-index-data data part-size)
         index      (keys index-data)]
     (save-index! path index)
     (doseq [[fname ks] index-data]
       (nippy/freeze-to-file (str path "/" fname)
                             (select-keys data ks))))))


(defn load [path]
  (reduce
   (fn [acc fname]
     (merge acc (nippy/thaw-from-file (str path "/" fname))))
   {}
   (load-index! path)))
