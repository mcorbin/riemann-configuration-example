(ns mycorp.output.email
  "send email"
  (:require [riemann.config :refer :all]
            [riemann.streams :refer :all]
            [riemann.test :refer :all]
            [riemann.email :refer :all]
            [clojure.tools.logging :refer :all]))

;; this stream can be used to send email
(def email (mailer {:from "me@mcorbin.fr"
                    :host "mail.foo.com"
                    :user "foo"
                    :password "bar"}))
