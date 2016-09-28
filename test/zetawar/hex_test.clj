(ns zetawar.hex-test
  (:require
   [clojure.spec.test :as stest]
   [clojure.test :refer [deftest testing is use-fixtures]]
   [clojure.test.check :as tc]
   [zetawar.hex :as hex]
   [zetawar.hex-spec]))

(deftest east-test
  (let [res (-> (stest/check 'zetawar.hex/east)
                first
                stest/abbrev-result)]
    (is (nil? (:failure res)))))
