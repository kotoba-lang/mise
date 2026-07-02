(ns mise.shipping-validation-test
  "Shipping address validation: required fields + email/postal format."
  (:require [clojure.test :refer [deftest is testing]]
            [mise.checkout :as co]))

(def valid-ship {:address "1-2-3" :city "Tokyo" :postal "100-0001" :country "JP"})
(def blank-ship {:address "" :city "" :postal "" :country ""})

(deftest valid-shipping-test
  (is (co/shipping-valid? valid-ship))
  (is (empty? (co/validate-shipping-address valid-ship))))

(deftest missing-fields-test
  (let [errs (co/validate-shipping-address blank-ship)]
    (is (= "required" (:address errs)))
    (is (= "required" (:city errs)))
    (is (= "required" (:postal errs)))
    (is (= "required" (:country errs)))
    (is (not (co/shipping-valid? blank-ship)))))

(deftest postal-format-test
  (is (co/postal-valid? "100-0001"))
  (is (co/postal-valid? "94105"))
  (is (not (co/postal-valid? "ab")))           ; too short
  (is (not (co/postal-valid? ""))))             ; blank

(deftest email-format-test
  (is (co/email-valid? "alice@gftd.ai"))
  (is (co/email-valid? "a@b.com"))
  (is (not (co/email-valid? "nope")))
  (is (not (co/email-valid? "a@b"))))           ; no TLD dot

(deftest invalid-postal-in-errors-test
  (let [errs (co/validate-shipping-address
              {:address "1" :city "X" :postal "xx" :country "JP"})]
    (is (= "invalid format" (:postal errs)))))   ; "xx" fails postal-valid?
