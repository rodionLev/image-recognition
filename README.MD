Run from project root with gradlew and custom configuration:

`./gradlew run -Dconfig=/home/user1/receipt.conf`

Or from distribution dir (gradle assembled):

  `export IMAGE_RECOGNITION_OPTS=-Dconfig=/home/user1/receipt.conf`
  `./image-recognition`

Default configuration file is ('services.azure.apiKey' is missing here):

  ```
  {
   provider = "azure"

   files {
     #list of input files/folders
     input: ["./input/"]
     #output folder
     output: "./output"
   }

   services{
     names {
       "azure": "com.demo.azure.service.AzureOcrService"
     },
     azure{
       endpoint:"https://centralus.api.cognitive.microsoft.com/vision/v2.0/recognizeText"
       #list of retry body contents
       notReadyResponses: ["{\"status\":\"NotStarted\"}", "{\"status\":\"Running\"}"]
     }
   }

   ```

To provide your Azure API key just create a file with content (it content will be merged with the default above):

```

{
services.azure.apiKey="someKey"

#overrides input default paths
files {
   #list of input folders
   input: ["/home/user2/input/"]
   #output folder
   output: "/home/user2/output"
 }
}

```

and pass path to it with jvm param '-Dconfig=/home/user1/receipt.conf'

or just override some configuration keys with command line directly:
`./gradlew run -Dservices.azure.apiKey="someKey" -Dfiles.input.0=/data/r-input`
