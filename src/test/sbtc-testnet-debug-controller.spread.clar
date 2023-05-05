;; THIS CONTRACT SHOULD ONLY BE USED FOR DEVELOPMENT PURPOSES
;; From: https://github.com/Trust-Machines/core-eng/blob/main/sbtc-mini/contracts/sbtc-testnet-debug-controller.clar
;;
;; Debug controller contract that can be made part
;; of the protocol during deploy.
;; This contract can trigger protocol upgrades
;; by the contract deployer or any principals it
;; defines later.

;; Add some safety to prevent accidental deployment on mainnet
(asserts!
 (is-eq
  chain-id u2147483648)
 (err "This contract can be deployed on testnet only"))

(define-constant
 err-not-debug-controller
 (err u900))

(define-map
 debug-controllers
 principal
 bool)
(map-set
 debug-controllers
 tx-sender
 true)

(define-read-only
 (is-debug-controller
  (controller
   principal))
 (ok
  (asserts!
   (default-to
    false
    (map-get?
     debug-controllers
     controller))
   err-not-debug-controller)))

;; #[allow(unchecked_data)]
(define-public
 (set-debug-controller
  (who
   principal)
  (enabled
   bool))
 (begin
  (try!
   (is-debug-controller
    tx-sender))
  (ok
   (map-set
    debug-controllers
    who
    enabled))))

;; #[allow(unchecked_data)]
(define-public
 (set-protocol-contract
  (contract
   principal)
  (enabled
   bool))
 (begin
  (try!
   (is-debug-controller
    tx-sender))
  (contract-call?
   .sbtc-controller
   upgrade
   (list
    {contract: contract,
     enabled: enabled}))))