LSAT Lightning Policy plugin
=
This [Apiman](https://www.apiman.io/latest/) plugin provides authentication based on the [LSAT specification](https://docs.lightning.engineering/the-lightning-network/lsat).  
Currently only LND lightning node is supported.

LSAT
-
This plugin provides the native LSAT-specification and a proprietary 'Boltwall' flow.
LSAT is based on the invoice pre-image, this pre-image is proof which only the payer receives after invoice payment.

Boltwall flow
-
[Boltwall](https://github.com/Tierion/boltwall) does not use a pre-image, but an additional request to sign a challenge (related to the paymentHash) by the lightning node.
This signature can be verified by the resource owner, and counts also as proof that the invoice has been paid.
The benefit of this flow is that the wallet, which paid the invoice, is primarily a mobile wallet and is not connected to the browser.
The additional round-trip is therefore required, because the browser does not have access to the pre-image.

- LSAT is useful for server-to-server api usage, connected to a local Lighting Wallet/Node.
- [WebLN](https://webln.dev/) is useful when a Lighting wallet is integrated by a browser extension/plugin. (e.g. [Joule](https://lightningjoule.com/))
- Boltwall is useful in a browser without an extension, but an external/mobile Lighting wallet 

![](img.png "LSAT Configuration")



TODO
-
Pricer plugin to provide the satoshi price per request

### Sample requests

1. request for protected resource:   
curl https://localhost:8443/apiman-gateway/LSAT.com/v1/1.0
```json
{
    "type":"Authorization",
    "failureCode":402,
    "responseCode":402,
    "message":"Payment required.",
    "headers":{
      "WWW-Authenticate":"LSAT macaroon=\"MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW50aWZpZXIgMDAwMDExMTkwYmI4YTEyNWRlZWJkYjhkMWU2YTA2YjgxYTQ4MjFiZWM1YjI2M2ZkN2UwZjA2ZGE1OTA2ODQwZmFjYmI1M2VjNjMyOTEzZTc1ZTc1NjMwMDI4MWMxNDQ1NWE2MGQ0NWYxM2RlNGNlNWNkNTBlZWY5MWEyMzFiYjg0YTE1CjAwNmRjaWQgY2hhbGxlbmdlPWhSVGNKaUFFR0hQUHVodDdpT1BHejJXblk5ZFAwSlZqYjNlbzBqSzJkZWs9OkU2NlZnaU9xWUhQaEtDbWEzUG45YlVPRUZPem15UVgzSmJFekdoMDFINEE9OgowMDJmc2lnbmF0dXJlILDrWgwfS377Q65zT92638s75e584I1-N492d2vgnA3DCg\", invoice=\"lnbcrt1u1psang46pp5zyvshw9pyh0whkudre4qdwq6fqsma3djv07hurcxmfvsdpq04jasdqqcqzpgxqyz5vqsp5zwhftq3r4fs88cfg9xdde70ad4pcg98vumystae9kye358f4r7qq9qyyssq2v0r9duldcsx8dhfvp39yrrqwu5tfgx2g47mum5qzj792f7r9zg46947uh556x43x2llpm5u2m2cfha82p6432f96vv2cdyfjz6zn6qphd90n3\""
    }
}
```

**When paid**

2. request with authorization with LSAT (pre-image)   
   curl https://localhost:8443/apiman-gateway/LSAT.com/v1/1.0 -H 'authorization: LSAT MDAzNmxvY2F0aW9uIGh0dHBzOi8vbHNhdC...:`<pre-image>`'
   
or

3. Boltwall request to sign challenge   
curl -d'{"macaroon":"MDAxMmxvY2F0aW9uIGxzYXQ..."}' -X POST /token/1.0
```json
{ "macaroon":"MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW..." }
```

4. request with signed challenge within the macaroon   
curl https://localhost:8443/apiman-gateway/LSAT.com/v1/1.0 -H 'authorization: LSAT MDAxMmxvY2F0aW9uIGxzYXQKMDA5NGlkZW...'
