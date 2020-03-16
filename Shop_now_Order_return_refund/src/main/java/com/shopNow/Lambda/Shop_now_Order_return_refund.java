package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Properties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Shop_now_Order_return_refund implements RequestHandler<JSONObject, JSONObject> {

	private String USERNAME;
	private String PASSWORD;
	private String DB_URL;

	@SuppressWarnings({ "unchecked", "unused" })
	public JSONObject handleRequest(JSONObject input, Context context) {

		LambdaLogger logger = context.getLogger();
		String Str_msg = null;

		JSONArray cart_Add_array = new JSONArray();
		JSONObject jsonObject_cancelorder_result = new JSONObject();

		Object userid1 = input.get("userid").toString();
		String return_id = input.get("return_id").toString();
		String order_id = input.get("order_id").toString();

		DecimalFormat df = new DecimalFormat("0.00");

		String orderNumber = null;
		Connection conn = null;
		int flag = 0;
		float sale_price = 0, grant_total = 0, shipping_charge = 0;
		float refund_grant_total = 0, refund_shipping_charge = 0;
		String refund_transaction_id = null, refund_payment_status = null, refund_mode_of_payment = null,
				refund_date_of_order_paid = null;

		String refund_type = "order_return";
		String refund_status = "not yet initiated";
		String sql_refund_insert = null;

		if (userid1 == null || userid1 == "") {

			Str_msg = "userID is null";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}
		long userid = Long.parseLong(userid1.toString());

		if (order_id == null || order_id == "") {

			Str_msg = "OrderID is  null";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}

		if (return_id == null || return_id == "") {

			Str_msg = "return_id is  null";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}
		// -----database connection-----------

		String order_number = null;
		int quantity = 0, shipping_address_id = 0;

		String sql_cust_order_product;
		String sql_order_update = null;
		String sql_cust_order_product_count;
		String sql_pro = null;
		int count = 0;
		int ved_id = 0;
		int order_product_vendor_id = 0;
		float discount, grand_sub_total;

		Properties prop = new Properties();

		try {
			prop.load(getClass().getResourceAsStream("/application.properties"));
			DB_URL = prop.getProperty("url");
			USERNAME = prop.getProperty("username");
			PASSWORD = prop.getProperty("password");
			conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
		} catch (Exception e) {
			e.printStackTrace();
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;

		}

		// Get time from DB server
		try {

			Statement stmt_customer = conn.createStatement();
			ResultSet srs_customer = stmt_customer
					.executeQuery("SELECT id, status FROM customers where id='" + userid + "'");

			if (srs_customer.next() == false) {

				Str_msg = "No user found!";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			}

			else if (!srs_customer.getString("status").equalsIgnoreCase("1")) {
				Str_msg = "User not confirmed !";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			}

			srs_customer.close();
			stmt_customer.close();

			String Sql_order_user = "select * from customer_orders where order_id='" + order_id + "' and customer_id='"
					+ userid + "'";
			logger.log("\n Sql_order_cancel \n" + Sql_order_user);
			Statement stmt_user = conn.createStatement();
			ResultSet srs_order_user = stmt_user.executeQuery(Sql_order_user);

			if (srs_order_user.next() == false) {

				Str_msg = "user have no order";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("return_request")) {
				flag = 1;
				logger.log("\n Order satatus delivered : \n ");
				discount = srs_order_user.getFloat("discounts");
				grand_sub_total = srs_order_user.getFloat("sub_total");

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("delivered")) {
				flag = 0;
				logger.log("\n Order satatus delivered : \n ");
				discount = srs_order_user.getFloat("discounts");
				grand_sub_total = srs_order_user.getFloat("sub_total");

			}

			else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("Return")) {
				Str_msg = "order already return";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("return_proccess_start")) {
				Str_msg = "order already return_proccess_start";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("cancel")) {
				Str_msg = "order already canceled So now You cant generate Return Rquest";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else {

				Str_msg = "order not return_request So now You cant get refund amount";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			}

			srs_order_user.close();
			stmt_user.close();
 
			String sql_order_return_refund = "SELECT * FROM return_table where return_id='" + return_id+ "'";
			logger.log("\n sql_order_return_refund : \n " + sql_order_return_refund);
			Statement stmt_order_return_refund = conn.createStatement();
			ResultSet resultset_order_return_refund = stmt_order_return_refund.executeQuery(sql_order_return_refund);
			int count_flag = 0;

			String product_ids = null, order_return_reason = null;
			if (resultset_order_return_refund.next()) {
				
				String return_option =resultset_order_return_refund.getString("return_option");
				
				if(return_option.equalsIgnoreCase("Refunded")) {

				product_ids = resultset_order_return_refund.getString("product_id");
				order_return_reason = resultset_order_return_refund.getString("order_return_reason");
				if (order_return_reason.contains("'")) {
					order_return_reason = order_return_reason.replaceAll("'", "''");

				}
				}
				else if(return_option.equalsIgnoreCase("Exchanged")){
					
					Str_msg = "order return_option Exchanged Comming Soon !";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
					
				}else if(return_option.equalsIgnoreCase("Replace")){
					
					Str_msg = "order return_option Replace Comming Soon !";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
					
				}

				

			} else {

				Str_msg = "order not return_request generated or product return process start So now You cant get refund amount";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			}

			
			
			
			
			String order_return_refund_detail = "select * from customer_order_details where product_id in ("
					+ product_ids + ") and order_id='"+order_id+"'";
			
			logger.log("\n order_return_refund_detail \n  "+order_return_refund_detail);
			Statement stmt_order_return_refund_detail = conn.createStatement();
			ResultSet resultset_order_return_refund_detail = stmt_order_return_refund_detail
					.executeQuery(order_return_refund_detail);

			long sale_price_total = 0, product_discount = 0;

			while (resultset_order_return_refund_detail.next()) {
				
				String delivery_status_code=resultset_order_return_refund_detail.getString("delivery_status_code");			
				logger.log("\n delivery_status_code \n " + delivery_status_code);
				 
				if(delivery_status_code.equalsIgnoreCase("return_proccess_start")) {
					
					Str_msg = "order already return_request process started";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
									
				}else if(delivery_status_code.equalsIgnoreCase("return_request")) {
					

					long refund_sale_price = resultset_order_return_refund_detail.getLong("price");
					// long quantity1=resultset_order_return_refund_detail.getInt("");
					long effective_price = resultset_order_return_refund_detail.getLong("effective_price");
					sale_price_total = sale_price_total + refund_sale_price;
					long refund_shipping_discounts = resultset_order_return_refund_detail.getLong("discounts");
					product_discount = product_discount + refund_shipping_discounts;
					refund_grant_total = refund_grant_total + effective_price;
					order_number = resultset_order_return_refund_detail.getString("order_number");
					
				}
				else {
							
					Str_msg = "order not return_request genereted  So you cannot call refund web service";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

					
				}

			}

			resultset_order_return_refund_detail.close();
			stmt_order_return_refund_detail.close();

			Statement stmt_refund_insert = conn.createStatement();
			sql_refund_insert = "insert into refund(customer_id,order_id,order_number,refund_grant_total,refund_status,refund_type,product_id,order_cancel_reason,discounts,shipping)values"
					+ "('" + userid + "','" + order_id + "','" + order_number + "','" + refund_grant_total + "','"
					+ refund_status + "','" + refund_type + "','" + product_ids + "','" + order_return_reason + "',"
					+ product_discount + "," + refund_shipping_charge + ")";

			logger.log("\n sql_refund_insert \n" + sql_refund_insert);

			int insert_add = stmt_refund_insert.executeUpdate(sql_refund_insert);
			stmt_refund_insert.close();

			String sql_order_update1;
			if (flag == 1) {
				sql_order_update1 = "UPDATE customer_orders SET order_status_code='return_proccess_start', grand_total=grand_total-" + refund_grant_total
						+ ",discounts=discounts -" + product_discount + " WHERE order_id='" + order_id + "'";
			} else {

				sql_order_update1 = "UPDATE customer_orders SET  grand_total=grand_total-" + refund_grant_total
						+ ",discounts=discounts -" + product_discount + " WHERE order_id='" + order_id + "'";

			}
			logger.log("\n sql_order_update \n" + sql_order_update1);
			Statement stmt_order_del1 = conn.createStatement();
			stmt_order_del1.executeUpdate(sql_order_update1);
			stmt_order_del1.close();

			String sql_update_order_detail;

			sql_update_order_detail = "update customer_order_details set delivery_status_code='return_proccess_start',expected_date_of_delivery=NULL where order_id='"
					+ order_id + "' and product_id in (" + product_ids + ")";

			logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);

			Statement stmt_cust_del = conn.createStatement();
			stmt_cust_del.executeUpdate(sql_update_order_detail);
			stmt_cust_del.close();

			String return_refund_detail = "select * from refund where order_id='" + order_id + "' and product_id ='"
					+ product_ids + "'";
			Statement stmt_return_refund_detail = conn.createStatement();
			ResultSet resultset_return_refund_detail = stmt_return_refund_detail.executeQuery(return_refund_detail);

			int refund_id = 0;
			if (resultset_return_refund_detail.next()) {

				refund_id = resultset_return_refund_detail.getInt("refund_id");

			}
			resultset_return_refund_detail.close();
			stmt_return_refund_detail.close();

			String sql_update_return_table = "update return_table set refund_id='" + refund_id + "' where return_id='"
					+ return_id + "'";
			logger.log("\n sql_cust_order_product_del \n" + sql_update_return_table);
			Statement stmt_return_table = conn.createStatement();
			stmt_return_table.executeUpdate(sql_update_return_table);
			stmt_return_table.close();

		} catch (

		Exception e) {
			e.printStackTrace();
			logger.log(e.toString());
			JSONObject jo_catch = new JSONObject();
			jo_catch.put("Exception", e.getMessage());
			return jo_catch;

		} finally {
			if (conn != null) {
				try {
					if (!conn.isClosed()) {

						conn.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					JSONObject jo_catch = new JSONObject();
					jo_catch.put("Exception", e.getMessage());
					return jo_catch;
				}
			}
		}

		jsonObject_cancelorder_result.put("status", "1");
		jsonObject_cancelorder_result.put("message", "Refund Proccess start");
		return jsonObject_cancelorder_result;

	}

}
