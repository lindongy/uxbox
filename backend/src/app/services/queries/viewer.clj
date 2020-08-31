;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; This Source Code Form is "Incompatible With Secondary Licenses", as
;; defined by the Mozilla Public License, v. 2.0.
;;
;; Copyright (c) 2020 UXBOX Labs SL

(ns app.services.queries.viewer
  (:require
   [app.common.exceptions :as ex]
   [app.common.spec :as us]
   [app.db :as db]
   [app.services.queries :as sq]
   [app.services.queries.files :as files]
   [clojure.spec.alpha :as s]))

;; --- Query: Viewer Bundle (by Page ID)

(def ^:private
  sql:project
  "select p.id, p.name
     from project as p
    where p.id = ?
      and p.deleted_at is null")

(defn- retrieve-project
  [conn id]
  (db/exec-one! conn [sql:project id]))

(s/def ::id ::us/uuid)
(s/def ::file-id ::us/uuid)
(s/def ::page-id ::us/uuid)
(s/def ::share-token ::us/string)

(s/def ::viewer-bundle
  (s/keys :req-un [::file-id ::page-id]
          :opt-un [::profile-id ::share-token]))

(sq/defquery ::viewer-bundle
  [{:keys [profile-id file-id page-id share-token] :as params}]
  (db/with-atomic [conn db/pool]
    (let [file    (files/retrieve-file conn file-id)
          project (retrieve-project conn (:project-id file))
          page    (get-in file [:data :pages-index page-id])]
      (if (string? share-token)
        (when (not= share-token (:share-token page))
          (ex/raise :type :validation
                    :code :not-authorized))
        (files/check-edition-permissions! conn profile-id file-id))
      {:file (dissoc file :data)
       :page (get-in file [:data :pages-index page-id])
       :project project})))
