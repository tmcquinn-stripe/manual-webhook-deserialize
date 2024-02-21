Use this sample code to handle webhook events in your integration.

<h2>Setup a webhook endpoint</h2>	
Collect & Confirm (Debit Detection) and forms on reader are beta features and require a webhook endpoint to be created via the API.  
<br>
<br>
Use the following cURL command to create a valid webhook endpoint

```
curl -X POST "https://api.stripe.com/v1/webhook_endpoints" \
  -u 'REPLACE_WITH_YOUR_SECRET_KEY':  \
  -H "Stripe-Version: 2022-11-15;terminal_collect_confirm_beta=v1;terminal_collect_inputs_beta=v1" \
  -d "url"="REPLACE_WITH_URL" \
  -d "enabled_events[0]"="terminal.reader.action_failed" \
  -d "enabled_events[1]"="terminal.reader.action_succeeded" \
  -d "enabled_events[2]"="terminal.reader.action_updated" \
  -d "api_version"="2022-11-15;terminal_collect_confirm_beta=v1;terminal_collect_inputs_beta=v1"
```

<br>
Build and run the server with, app is hosted on 4242:  

```
mvn compile
mvn exec:java -Dexec.mainClass=com.example.Server
```
