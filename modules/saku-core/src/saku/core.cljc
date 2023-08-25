(ns saku.core
  (:require [saku.utils :as utils]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Evalutation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:private deny-evaluation? [{:keys [action
                                          drn
                                          resource-policy
                                          identity-policies-map] :as request}]
  (or (some (fn [{:keys [actions identities effect]:as statement}]
              (and (= :DENY (keyword effect))
                   (utils/star-match-one-to-many action actions)
                   (utils/star-match-many-to-many (keys identity-policies-map) identities)))
            (:statements resource-policy))
      (some (fn [[identity-drn identity-policy]]
              (some (fn [{:keys [actions resources effect]:as statement}]
                      (and (= :DENY (keyword effect))
                           (utils/star-match-one-to-many action actions)
                           (utils/star-match-one-to-many drn resources)))
                    (:statements identity-policy)))
            identity-policies-map)))

(defn ^:private resource-based-allow? [{:keys [action
                                               drn
                                               resource-policy
                                               identity-policies-map] :as request}]
  (some (fn [{:keys [actions identities effect]:as statement}]
          (and (= :ALLOW (keyword effect))
               (utils/star-match-one-to-many action actions)
               (utils/star-match-many-to-many (keys identity-policies-map) identities)))
        (:statements resource-policy)))

(defn ^:private identity-based-allow? [{:keys [action
                                               drn
                                               resource-policy
                                               identity-policies-map] :as request}]
  (some (fn [[identity-drn identity-policy]]
          (some (fn [{:keys [actions resources effect]:as statement}]
                  (and (= :ALLOW (keyword effect))
                       (utils/star-match-one-to-many action actions)
                       (utils/star-match-one-to-many drn resources)))
                (:statements identity-policy)))
        identity-policies-map))

;; returns :ALLOW or :DENY (final effect) of desired action over target drn
(defn evaluate-one [request]
  (cond
    (deny-evaluation? request)
    {:effect :DENY :nature :EXPLICIT}

    (resource-based-allow? request)
    {:effect :ALLOW :nature :EXPLICIT}

    (identity-based-allow? request)
    {:effect :ALLOW :nature :EXPLICIT}

    :else
    {:effect :DENY :nature :IMPLICIT}))

(defn evaluate-many [{:keys [action
                             drn
                             resource-policies
                             identity-policies-map] :as request}]
  (reduce (fn [a resource-policy]
            (assoc a (:drn resource-policy)
                   (evaluate-one (-> request
                                     (dissoc :resource-policies)
                                     (assoc :resource-policy resource-policy)))))
          {} resource-policies))


#?(:cljs
   (defn ^:private sanitize-evaluate-one-input [params]
     (let [{:keys [drn action
                   resourcePolicy
                   identityPoliciesMap]} (-> params
                                             (js->clj :keywordize-keys true))]
       {:drn drn
        :action action
        :resource-policy resourcePolicy
        :identity-policies-map identityPoliciesMap})))


#?(:cljs
   (defn evaluate-one-js [params]
     (-> params
         sanitize-evaluate-one-input
         evaluate-one
         clj->js)))


#?(:cljs
   (defn ^:private sanitize-evaluate-many-input [params]
     (let [{:keys [drn action
                   resourcePolicies
                   identityPoliciesMap]} (-> params
                                             (js->clj :keywordize-keys true))]
       {:drn drn
        :action action
        :resource-polices resourcePolicies
        :identity-policies-map identityPoliciesMap})))


#?(:cljs
   (defn ^:export evaluate-many-js [params]
     (-> params
         sanitize-evaluate-many-input
         evaluate-many
         clj->js)))
