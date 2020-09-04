;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; This Source Code Form is "Incompatible With Secondary Licenses", as
;; defined by the Mozilla Public License, v. 2.0.
;;
;; Copyright (c) 2020 UXBOX Labs SL

(ns app.tests.test-services-projects
  (:require
   [clojure.test :as t]
   [promesa.core :as p]
   [app.db :as db]
   [app.http :as http]
   [app.services.mutations :as sm]
   [app.services.queries :as sq]
   [app.tests.helpers :as th]
   [app.common.uuid :as uuid]))

(t/use-fixtures :once th/state-init)
(t/use-fixtures :each th/database-reset)

(t/deftest projects-crud
  (let [prof (th/create-profile db/pool 1)
        team (th/create-team db/pool (:id prof) 1)
        project-id (uuid/next)]

    (t/testing "create a project"
      (let [data {::sm/type :create-project
                  :id project-id
                  :profile-id (:id prof)
                  :team-id (:id team)
                  :name "test project"}
            out (th/try-on! (sm/handle data))]
        ;; (th/print-result! out)

        (t/is (nil? (:error out)))
        (let [result (:result out)]
          (t/is (= (:name data) (:name result))))))

    (t/testing "query a list of projects"
      (let [data {::sq/type :projects
                  :team-id (:id team)
                  :profile-id (:id prof)}
            out (th/try-on! (sq/handle data))]
        ;; (th/print-result! out)

        (t/is (nil? (:error out)))
        (let [result (:result out)]
          (t/is (= 1 (count result)))
          (t/is project-id (get-in result [0 :id]))
          (t/is "test project" (get-in result [0 :name])))))

    (t/testing "rename project"
      (let [data {::sm/type :rename-project
                  :id project-id
                  :name "renamed project"
                  :profile-id (:id prof)}
            out  (th/try-on! (sm/handle data))]
        ;; (th/print-result! out)
        (t/is (nil? (:error out)))
        (let [result (:result out)]
          (t/is (= (:id data) (:id result)))
          (t/is (= (:name data) (:name result)))
          (t/is (= (:profile-id data) (:id prof))))))

    (t/testing "delete project"
      (let [data {::sm/type :delete-project
                  :id project-id
                  :profile-id (:id prof)}
            out  (th/try-on! (sm/handle data))]

        ;; (th/print-result! out)
        (t/is (nil? (:error out)))
        (t/is (nil? (:result out)))))

    (t/testing "query a list of projects after delete"
      (let [data {::sq/type :projects
                  :team-id (:id team)
                  :profile-id (:id prof)}
            out (th/try-on! (sq/handle data))]
        ;; (th/print-result! out)

        (t/is (nil? (:error out)))
        (let [result (:result out)]
          (t/is (= 0 (count result))))))
    ))
