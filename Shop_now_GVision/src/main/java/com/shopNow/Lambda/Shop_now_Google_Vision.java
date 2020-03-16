package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.WebDetection.WebEntity;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
 

public class Shop_now_Google_Vision implements RequestHandler<JSONObject, JSONObject> {

	@SuppressWarnings({ "unused", "unchecked", "restriction" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();
		logger.log("Invoked JDBCSample.getCurrentTime");

		String Str_msg;
		String imageUrl = null;
		JSONObject jSONObject = new JSONObject();
		JSONObject imgResponses = new JSONObject();
		JSONObject final_imgResponses = new JSONObject();
		Object image_url = input.get("image_url");
	    Object byte_data = input.get("data");
		String data = input.get("data").toString();
		int flag = 0;
		if (image_url == null || image_url == "") {
			flag = 1;
		} else {
			flag = 0;
			imageUrl = image_url.toString();
		}

		/*
		 * String imgIndex = imageUrl.substring(0, 4); String http1 = "http";
		 */
		ByteString imageBytes = null;
		if (flag == 0) {
			URL url = null;
			try {
				url = new URL(imageUrl);
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
				logger.log(e.toString());
			}

			byte[] img = baos.toByteArray();
			imageBytes = ByteString.copyFrom(img);

			logger.log("\nHttp Image is converted into Byte Array");

		} else {			
			if (data == null || data == "") {

				Str_msg = "Image_Url and Data both cannot be null";
				imgResponses.put("status", "0");
				imgResponses.put("message", Str_msg);

				return imgResponses;
			} else {
				byte[] img = javax.xml.bind.DatatypeConverter.parseBase64Binary(data);
				imageBytes = ByteString.copyFrom(img);
				logger.log("\nLocal Image is converted into Byte Array");
			}
		}

		BatchAnnotateImagesResponse responses;
		Image image = Image.newBuilder().setContent(imageBytes).build();
		logger.log("\nImage Object is builded");
		// Sets the type of request to label detection, to detect broad sets of
		// categories in an image.

		List<Feature> featureList = new ArrayList<Feature>();
		Feature labelDetectionFeature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
		featureList.add(labelDetectionFeature);
		logger.log("\nLabelDetectionFeature is builded");

		Feature webDetectionFeature = Feature.newBuilder().setType(Feature.Type.WEB_DETECTION).build();
		featureList.add(webDetectionFeature);
		logger.log("\nWebDetectionFeature is builded");

		Feature imagePropertiesFeature = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
		featureList.add(imagePropertiesFeature);
		logger.log("\nImagePropertiesFeature is builded");

		AnnotateImageRequest annotateImageRequest = AnnotateImageRequest.newBuilder().setImage(image)
				.addAllFeatures(featureList).build();
		logger.log("\nAnnotateImageRequest is builded\n");
		try {

			ImageAnnotatorClient client = ImageAnnotatorClient.create();

			responses = client.batchAnnotateImages(Collections.singletonList(annotateImageRequest));

			// We're only expecting one response.
			AnnotateImageResponse response = responses.getResponses(0);
			logger.log("\nReponse object is created");

			if (responses.getResponsesCount() == 1) {
				if (response.hasError()) {
					try {
						throw new Exception(response.getError().getMessage());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				JSONArray labelAnnotations = new JSONArray();
				logger.log("\nLabelAnnotation Object is executed ");
				for (EntityAnnotation annotation : response.getLabelAnnotationsList()) {
					JSONObject mid = new JSONObject();
					mid.put("mid", annotation.getMid());
					mid.put("description", annotation.getDescription());
					mid.put("score", annotation.getScore());
					mid.put("topicality", annotation.getTopicality());
					
					labelAnnotations.add(mid);

				}

				imgResponses.put("labelAnnotations", labelAnnotations);

				DominantColorsAnnotation dominantColor = response.getImagePropertiesAnnotation().getDominantColors();

				logger.log("\nImageProperties dominantColor Object is executed");
				JSONArray colors = new JSONArray();
				for (ColorInfo ColorInfo : dominantColor.getColorsList()) {

					JSONObject rgbcolor = new JSONObject();
					JSONObject color = new JSONObject();
					rgbcolor.put("red", ColorInfo.getColor().getRed());
					rgbcolor.put("green", ColorInfo.getColor().getGreen());
					rgbcolor.put("blue", ColorInfo.getColor().getBlue());
					color.put("color", rgbcolor);
					color.put("score", ColorInfo.getScore());
					color.put("pixelFraction", ColorInfo.getPixelFraction());
					
					colors.add(color);
				}
				JSONObject dominant_colors = new JSONObject();
				dominant_colors.put("dominantColors", colors);
				imgResponses.put("imagePropertiesAnnotation", dominant_colors);

				/* Web Detection */
				logger.log("\nWebDetection object is executed\n");

				JSONObject web = new JSONObject();
				JSONArray webEntities = new JSONArray();
				for (WebEntity webEntityList : response.getWebDetection().getWebEntitiesList()) {
					JSONObject web_detection = new JSONObject();
					web_detection.put("entityId", webEntityList.getEntityId());
					web_detection.put("score", webEntityList.getScore());
					web_detection.put("description", webEntityList.getDescription());
				
					webEntities.add(web_detection);

				}
				web.put("webEntities", webEntities);

				/* Visually Similar Images */
				int count = response.getWebDetection().getVisuallySimilarImagesCount();

				JSONArray visuallySimilarImages = new JSONArray();

				for (int i = 0; i < count; i++) {

					WebImage ar = response.getWebDetection().getVisuallySimilarImages(i);
					String VisImg = ar.toString();

					JSONObject url = new JSONObject();

					url.put("url", ar.toString().substring(6, ar.toString().length() - 2));
					
					visuallySimilarImages.add(url);
				}
				web.put("visuallySimilarImages", visuallySimilarImages);
				JSONArray bestGuessLabels = new JSONArray();

				int lblCount = response.getWebDetection().getBestGuessLabelsCount();
				for (int i = 0; i < lblCount; i++) {
					JSONObject bestGLabels = new JSONObject();
					bestGLabels.put("label", response.getWebDetection().getBestGuessLabels(i).getLabel());
					
					bestGuessLabels.add(bestGLabels);
				}
				web.put("bestGuessLabels", bestGuessLabels);
				imgResponses.put("webDetection", web);

			}
			final_imgResponses.put("Responses", imgResponses);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.log("\n\nInside IOException Catch Block\n\n");
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.log("\n\nInside Exception Catch Block\n\n");
			e1.printStackTrace();
		}

		return final_imgResponses;
	}

	
}