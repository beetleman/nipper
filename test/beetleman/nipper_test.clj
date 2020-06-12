(ns beetleman.nipper-test
  (:require [beetleman.nipper :as sut]
            [beetleman.nipper.data :as data]
            [clojure.test :as t]))


(t/deftest dump!-load!-test
  (let [path ".test/dump-load-test-1"
        m    (data/generate 4 4)
        d    (with-meta (data/generate 100) m)]
    (t/is (= path
             (sut/dump! path d {:part-size 3})))
    (let [loaded (sut/load! path)]
      (t/testing "d -> dump! -> load! -> d"
                 (t/is (= d
                          loaded)))
      (t/testing "metadata survive"
                 (t/is (= m
                          (meta loaded)))))
    (t/testing
     "progress-cb"
     (let [load-cb (atom [])
           dump-cb (atom [])
           path    (sut/dump! ".test/dump-load-test-2"
                              d
                              {:progress-cb #(swap! dump-cb conj %)})
           loaded  (sut/load! path {:progress-cb #(swap! load-cb conj %)})]
       (t/is (= d
                loaded))
       (t/is (= @load-cb
                @dump-cb))
       (t/is (zero? (first @load-cb)))
       (t/is (= 100
                (last @load-cb)))))
    (t/testing
     "force-parts"
     (let [path   (sut/dump! ".test/dump-load-test-3"
                             d
                             {:force-parts [[::not-exist]
                                            [::not-exist-2
                                             (-> d
                                                 keys
                                                 rand-nth)]]})
           loaded (sut/load! path)]
       (t/is (= d
                loaded))))))


(t/deftest create-index-data-test
  (t/testing
   "create-index-data do not create dulicate or skip any of keys"
   (let [d     (data/generate 100)
         index (#'sut/create-index-data d 9 [])]
     (t/is (= (sort (keys d))
              (sort (mapcat identity
                     (vals index))))))
   (t/testing
    "with forced parts"
    (let [d     {:1 1
                 :2 2
                 :3 3
                 :4 4
                 :5 5
                 :6 6
                 :7 7}
          index (#'sut/create-index-data
                 d
                 2
                 [[::not-exists]
                  [:7]
                  [::not-exist-2
                   :2]])]
      (t/is (= {"3.npy" [:4 :5]
                "2.npy" [:1 :3]
                "1.npy" [:2] ;; forced
                "0.npy" [:7] ;; forced
                "4.npy" [:6]}
               index))))))
