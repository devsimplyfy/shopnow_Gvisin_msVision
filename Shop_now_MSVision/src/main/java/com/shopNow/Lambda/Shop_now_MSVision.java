package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Shop_now_MSVision implements RequestHandler<JSONObject, JSONObject> {

	@SuppressWarnings({ "unchecked", "unused", "restriction" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();
		logger.log("Invoked JDBCSample.getCurrentTime");

		String Str_msg;
		String imageUrl = null;

		JSONObject imgResponses = new JSONObject();

		Object image_url = input.get("image");
		Object byte_data = input.get("data");
		
		String data = input.get("data").toString();
		int flag = 0;

		if (image_url == null || image_url == "") {
			flag = 1;
		} else {
			flag = 0;
			imageUrl = image_url.toString();
		}
		String msVisionString = new String();
		File file1 = new File(getClass().getClassLoader().getResource("subscriptionKey.txt").getFile());

		String uriBase = "https://westcentralus.api.cognitive.microsoft.com/vision/v2.0/analyze";
		String subscriptionKey = null;
		try {
			subscriptionKey = FileUtils.readFileToString(file1, "UTF-8");
			logger.log(subscriptionKey);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		URIBuilder builder = null;
		try {
			builder = new URIBuilder(uriBase);
		} catch (URISyntaxException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		// Request parameters. All of them are optional.
		builder.setParameter("visualFeatures", "Categories,Description,Color,Tags");
		builder.setParameter("language", "en");
		logger.log(builder.toString());
		// Prepare the URI for the REST API call.
		URI uri = null;
		try {
			uri = builder.build();
		} catch (URISyntaxException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		HttpPost request = new HttpPost(uri);

		// String imgIndex = imageUrl.substring(0, 4);
		ByteBuffer imageBytes;
		if (flag == 0) {
			URL url = null;
			try {
				url = new URL(imageUrl);
				logger.log("\n URL : " + url);
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			URLConnection conn = null;
			try {
				conn = url.openConnection();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			conn.setRequestProperty("User-Agent", "Firefox");
			try {
				InputStream inputStream = conn.getInputStream();
				int n = 0;
				byte[] buffer = new byte[1024];
				while (-1 != (n = inputStream.read(buffer))) {
					baos.write(buffer, 0, n);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			byte[] img = baos.toByteArray();

			// Request body.
			StringEntity requestEntity;
			try {
				requestEntity = new StringEntity("{\"url\":\"" + imageUrl + "\"}");
				request.setEntity(requestEntity);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		   // request.setEntity(new ByteArrayEntity(img));
			request.setHeader("Content-Type", "application/json");
			request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

		} else {
			if (data == null || data == "") {

				Str_msg = "Image_Url and Data both cannot be null";
				imgResponses.put("status", "0");
				imgResponses.put("message", Str_msg);

				return imgResponses;
			} else {
				
				byte[] img = javax.xml.bind.DatatypeConverter.parseBase64Binary(data);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
				request.setEntity(new ByteArrayEntity(img));

				request.setHeader("Content-Type", "application/octet-stream"); // For Local File System Image

				request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);
			}
		}
		HttpResponse response = null;
		
		try {
			response = httpClient.execute(request);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpEntity entity = response.getEntity();

		if (entity != null) {

			try {

				msVisionString = EntityUtils.toString(entity);

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.log("REST Response:\n");
			logger.log("msVisionString " + msVisionString);
		}
		logger.log("EntityResponse Displayed");
		System.out.println(msVisionString);
		JSONParser parser = new JSONParser();
		try {
			JSONObject json = (JSONObject) parser.parse(msVisionString);
			imgResponses.put("msVisionString", json);
			return imgResponses;
		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		imgResponses.put("msVisionString", msVisionString);
		return imgResponses;
	}
}