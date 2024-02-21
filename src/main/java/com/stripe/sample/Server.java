// Server.java
//
// Use this sample code to handle webhook events in your integration.
//
// Using Maven:
// 1) Paste this code into a new file (src/main/java/com/stripe/sample/Server.java)
//
// 2) Create a pom.xml file. You can quickly copy one from an official Stripe Sample.
//   curl https://raw.githubusercontent.com/stripe-samples/accept-a-payment/8e94e50e68072c344f12b02f65a6240e1c656d4a/custom-payment-flow/server/java/pom.xml -o pom.xml
//
// 3) Build and run the server on http://localhost:4242
//   mvn compile
//   mvn exec:java -Dexec.mainClass=com.stripe.sample.Server

package com.stripe.sample;

import static spark.Spark.post;
import static spark.Spark.port;

import com.google.gson.*;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;

public class Server {
    public static void main(String[] args) {
        // The library needs to be configured with your account's secret key.
        // Ensure the key is kept out of any version control system you might be using.

        Stripe.apiKey = "{REPLACE WITH SECRET API KEY}";
        Stripe.stripeVersion += "; terminal_collect_confirm_beta=v1; terminal_collect_inputs_beta=v1";

        // This is your Stripe CLI webhook secret for testing your endpoint locally.
        String endpointSecret = "{REPLACE WITH WEBHOOK SIGNING SECRET}";

        port(4242);

        post("/webhook", (request, response) -> {
            String payload = request.body();
            String sigHeader = request.headers("Stripe-Signature");
            Event event = null;

            // The webhook signature still needs to be validated.
            try {
                event = Webhook.constructEvent(
                        payload, sigHeader, endpointSecret
                );
            } catch (JsonSyntaxException e) {
                // Invalid payload
                e.printStackTrace();
                response.status(400);
                return "";
            } catch (SignatureVerificationException e) {
                // Invalid signature
                e.printStackTrace();
                response.status(400);
                return "";
            } catch(Exception e) {
                e.printStackTrace();
            }

            // Deserialize the nested object inside the event
            JsonObject rootObj = JsonParser.parseString(event.toJson()).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("object");

            // Raw object
            System.out.println("ℹ️ Raw Object");
            System.out.println(rootObj.toString());
            System.out.println();

            // Root Action
            JsonObject action = rootObj.getAsJsonObject("action");

            // Status of Action
            String status = action.getAsJsonPrimitive("status").getAsString();

            // Type of Action
            String type = action.getAsJsonPrimitive("type").getAsString();

            switch (event.getType()) {
                case "terminal.reader.action_failed": {
                    System.out.println("⛔⛔⛔⛔⛔⛔ ACTION FAILED ⛔⛔⛔⛔⛔⛔");
                    if(getErrorState(status, action)) {
                        // Something happened
                        break;
                    }

                    handleAction(action, type, status);
                    System.out.println("⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔⛔");
                    System.out.println();

                    break;
                }
                case "terminal.reader.action_updated": {
                    // This is used for collect/confirm debit detection flow only
                    System.out.println("ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ Action Updated ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️");

                    if(getErrorState(status, action)) {
                        // Something happened
                        break;
                    }

                    handleAction(action, type, status);

                    System.out.println("ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️ℹ️");
                    System.out.println();

                    break;
                }
                case "terminal.reader.action_succeeded": {
                    System.out.println("✅✅✅✅✅✅ Action Succeeded ✅✅✅✅✅✅");

                    // Then define and call a function to handle the event terminal.reader.action_succeeded
                    if(getErrorState(status, action)) {
                        // Something happened
                        break;
                    }

                    handleAction(action, type, status);
                    System.out.println("✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅✅");
                    System.out.println();

                    break;
                }
                // ... handle other event types
                default:
                    System.out.println("Unhandled event type: " + event.getType());
            }
            response.status(200);
            return "";
        });
    }

    public static boolean getErrorState(String status, JsonObject action) {
        if (status.equalsIgnoreCase("failed")) {
            String failureCode = action.getAsJsonPrimitive("failure_code").getAsString();
            String failureMessage = action.getAsJsonPrimitive("failure_message").getAsString();

            System.err.println("Failure Code: " + failureCode);
            System.err.println("Failure Message: " + failureMessage);

            return true;
        }

        return false;
    }

    public static void getActionMetadata(JsonObject actionType) {
        JsonObject formMetadata;
        String raValue = null;
        String purpose = null;

        // metadata & traverse as needed for metadata tags.
        formMetadata = actionType.getAsJsonObject("metadata");
        if (formMetadata != null && !formMetadata.isJsonNull()) {
            raValue = formMetadata.getAsJsonPrimitive("rental_agreement").getAsString();
            purpose = formMetadata.getAsJsonPrimitive("purpose").getAsString();
        }

        // Do something with this information
        System.out.println("Rental Agreement: " + raValue + " | " + "Purpose: " + purpose );
        return;
    }

    public static void handleAction(JsonObject action, String type, String status) throws StripeException {
        switch (type) {
            case "collect_inputs":
                System.out.println("*********************** COLLECT INPUTS ***********************");

                JsonObject collectInputs = action.getAsJsonObject("collect_inputs");

                // retrieve metadata
                getActionMetadata(collectInputs);

                // Input traversal
                JsonArray inputs = collectInputs.getAsJsonArray("inputs");
                JsonObject input = inputs.get(0).getAsJsonObject();
                // Only displaying one form at a time - if you are displaying multiple without in 1 request,
                // you'll need a for loop
                String inputType = input.getAsJsonPrimitive("type").getAsString();

                if (inputType.equals("selection")) {
                    String selectionValue = input.getAsJsonObject("selection").getAsJsonPrimitive("value").getAsString();
                    // Do Something with the value, like write it to a DB.
                    System.out.println("selection value: " + selectionValue);
                }

                if (inputType.equals("signature")) {
                    String signatureFileId = input.getAsJsonObject("signature").getAsJsonPrimitive("value").getAsString();

                    String signatureFileUrl = File.retrieve(signatureFileId).getUrl();

                    // Download the file from URL leveraging Stripe secret API key.
                    System.out.println("Signature File URL: " + signatureFileUrl);
                }

                break;
            case "collect_payment_method":
                System.out.println("*********************** COLLECT PAYMENT METHOD ***********************");

                JsonObject collectPaymentMethod = action.getAsJsonObject("collect_payment_method");

                // Get metadata
                getActionMetadata(collectPaymentMethod);

                JsonObject cardPresent = collectPaymentMethod.getAsJsonObject("payment_method").getAsJsonObject("card_present");

                // Get read method
                String readMethod = cardPresent.getAsJsonPrimitive("read_method").getAsString();

                // Get if Apple Pay
                JsonObject wallet = cardPresent.getAsJsonObject("wallet");
                String walletType = null;

                if (wallet != null) {
                    walletType = wallet.getAsJsonPrimitive("type").getAsString();
                }

                // Get if debit card
                String fundingType = cardPresent.getAsJsonPrimitive("funding").getAsString();

                // Get cardholder name
                String cardHolderName = cardPresent.getAsJsonPrimitive("cardholder_name").getAsString();

                // Do something with the information.
                System.out.println("Cardholder name: " + cardHolderName + " | Funding Type: " + fundingType);
                System.out.println("Read Method: " + readMethod + " | Wallet: " + walletType);
                break;

            case "confirm_payment_intent":
                System.out.println("*********************** CONFIRM PAYMENT INTENT ***********************");
                JsonObject collectPaymentIntent = action.getAsJsonObject("confirm_payment_intent");

                // Only the payment intent ID is returned. To make things easier on yourself use the Stripe Java library
                // to retrieve the PaymentIntent, so you can expand fields as needed.
                String paymentIntentId = collectPaymentIntent.getAsJsonPrimitive("payment_intent").getAsString();

                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

                String piStatus = paymentIntent.getStatus();
                Long amount = paymentIntent.getAmount();
                String currency = paymentIntent.getCurrency();

                // Metadata is stored on the PaymentIntent itself, not the action.
                String rental_agreement = paymentIntent.getMetadata().get("rental_agreement");

                System.out.println("Payment Intent: " + paymentIntentId + " | Status: " + piStatus);
                System.out.println("RA: " + rental_agreement + " | Amount: " + amount + " | Currency: " + currency);
                break;
            default:
                System.out.println("Unexpected");
        }
        return;
    }
}