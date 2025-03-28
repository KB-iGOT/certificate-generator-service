## certificate-generator-service
Play, Akka seed project without router implementation.

certificate Generation service will run by default on port 9000.


### Build

1. Execute clean install `mvn clean install`


### Run 
1. For debug mode, <br> 
   `cd play-service` <br>
   `mvn play2:dist`  <br>
   `mvn play2:run`

2. For run mode, 
   `cd play-service` <br>
   `mvn play2:dist`  <br>
   `mvn play2:start`

### Verify running status

Hit the following Health check curl command 

`curl -X GET \
   http://localhost:9000/health \
   -H 'Postman-Token: 6a5e0eb0-910a-42d1-9077-c46f6f85397d' \
   -H 'cache-control: no-cache'`

And, a successful response must be like this:

`{"id":"api.200ok","ver":"v1","ts":"2019-01-17 16:53:26:286+0530","params":{"resmsgid":null,"msgid":"8e27cbf5-e299-43b0-bca7-8347f7ejk5abcf","err":null,"status":"success","errmsg":null},"responseCode":"OK","result":{"response":{"response":"SUCCESS","errors":[]}}}`
