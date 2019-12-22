(ns beetleman.nipper-test
  (:require [beetleman.nipper :as sut]
            [beetleman.nipper.data :as data]
            [clojure.test :as t]))


(t/deftest dump-load-test
  (let [path ".test/dump-load-test"
        d    (data/generate 100)]
    (t/is (nil?
           (sut/dump path d 3)))
    (t/is (= d
             (sut/load path)))))
