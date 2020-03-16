package com.shopNow.Lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Properties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Shop_now_Order_return implements RequestHandler<JSONObject, JSONObject> {

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
		String product_id1 = input.get("product_id").toString();
		String order_id = input.get("order_id").toString();
		String order_return_reason = input.get("order_return_reason").toString();
		if (order_return_reason.contains("'")) {
			order_return_reason = order_return_reason.replaceAll("'", "''");

		}
		if (order_return_reason.equalsIgnoreCase("NULL") || order_return_reason.equalsIgnoreCase("")) {

			Str_msg = "order_return_reason is null";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}
		
		
		String order_return_condition = input.get("order_return_condition").toString();

		if (order_return_condition.contains("'")) {
			order_return_condition = order_return_condition.replaceAll("'", "''");

		}
		
		if (order_return_condition.equalsIgnoreCase("NULL") || order_return_condition.equalsIgnoreCase("")) {

			Str_msg = "order_return_condition is null Please Select Valid order_return_condition like Opened OR Not Opened OR damaged ";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}
		else if(order_return_condition.equalsIgnoreCase("Opened")||order_return_condition.equalsIgnoreCase("Not Opened")||order_return_condition.equalsIgnoreCase("damaged")) {
			
			
		}
		else {
			
			Str_msg = "Select Valid order_return_condition like Opened OR Not Opened OR damaged ";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

			
			
		}

		String order_return_option = input.get("order_return_option").toString();
		if (order_return_option.equalsIgnoreCase("NULL") || order_return_option.equalsIgnoreCase("")) {

			Str_msg = "order_return_option is null Please Select Valid order_return_option like Exchanged OR refund OR Replace";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;

		}
		else if(order_return_option.equalsIgnoreCase("Exchanged")||order_return_option.equalsIgnoreCase("Refunded")||order_return_option.equalsIgnoreCase("Replace")) {
			
			
			
		}
		else {
			
			Str_msg = "Select Valid order_return_option like Exchanged OR Refunded OR Replace ";
			jsonObject_cancelorder_result.put("status", "0");
			jsonObject_cancelorder_result.put("message", Str_msg);
			return jsonObject_cancelorder_result;
		}

		String schedule_pickup = input.get("pickup_schedule").toString();
		if (schedule_pickup.equalsIgnoreCase("") || schedule_pickup.equalsIgnoreCase("NULL")) {

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now().plusDays(2);
			schedule_pickup = dtf.format(now);
		}
		
		

		DecimalFormat df = new DecimalFormat("0.00");

		String orderNumber = null;
		Connection conn = null;
		int flag = 0;
		float sale_price = 0, grant_total = 0, shipping_charge = 0;
		float refund_sale_price = 0, refund_grant_total = 0, refund_shipping_charge = 0, refund_shipping_discounts = 0;
		String refund_transaction_id = null, refund_payment_status = null, refund_mode_of_payment = null,
				refund_date_of_order_paid = null;

		String refund_type = "order_cancel";
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

		if (product_id1 == null || product_id1 == "") {
			flag = 0;

		} else {
			flag = 1;

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
			// logger.log("\n Sql_order_cancel \n" + Sql_order_user);
			Statement stmt_user = conn.createStatement();
			ResultSet srs_order_user = stmt_user.executeQuery(Sql_order_user);

			if (srs_order_user.next() == false) {

				Str_msg = "user have no order";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;
			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("delivered")) {

				logger.log("\n Order satatus delivered : \n ");
				discount = srs_order_user.getFloat("discounts");
				grand_sub_total = srs_order_user.getFloat("sub_total");

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("return_request")) {
				Str_msg = "order already Return Request generated";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("Return")) {
				Str_msg = "order already return";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else if (srs_order_user.getString("order_status_code").equalsIgnoreCase("cancel")) {
				Str_msg = "order already canceled So now You cant generate Return Rquest";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			} else {

				Str_msg = "order not delivered So now You cant generate Return Rquest ";
				jsonObject_cancelorder_result.put("status", "0");
				jsonObject_cancelorder_result.put("message", Str_msg);
				return jsonObject_cancelorder_result;

			}

			srs_order_user.close();
			stmt_user.close();

			String sql_order_product_unic1 = "SELECT count(*) as number_of_product_in_order FROM customer_order_details where order_id='"
					+ order_id + "'";
			// logger.log("\n sql_order_product_unic1 : \n " + sql_order_product_unic1);
			Statement stmt_cust_unic1 = conn.createStatement();
			ResultSet cust_order_product_unic1 = stmt_cust_unic1.executeQuery(sql_order_product_unic1);
			int count_flag = 0;
			if (cust_order_product_unic1.next()) {
				if (cust_order_product_unic1.getInt("number_of_product_in_order") == 1) {

					flag = 0;

				} else {

					count_flag = 1;

				}

			}

			if (flag == 1) {

				int product_id = 0, vendor_id1 = 0;
				float effective_sale_price = 0, discount_product = 0;
				float total_sub = 0, total_grand = 0, total_discount = 0, total_shipping = 0;

				logger.log("\n flag :" + flag + "\n");

				String Sql_order_cancel1 = "select * from customer_orders where order_id='" + order_id + "'";

				// logger.log("\n Sql_order_cancel \n" + Sql_order_cancel1);
				Statement stmt1 = conn.createStatement();
				ResultSet srs_order_cancel1 = stmt1.executeQuery(Sql_order_cancel1);

				if (srs_order_cancel1.next() == false) {

					Str_msg = "order is not present";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
				} else {
					if (srs_order_cancel1.getString("order_status_code").equalsIgnoreCase("cancel")) {

						Str_msg = "order already canceled";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					} else if (srs_order_cancel1.getString("order_status_code").equalsIgnoreCase("shipped")) {

						Str_msg = "order already shipped";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					}

					order_number = srs_order_cancel1.getString("order_number");
					shipping_address_id = srs_order_cancel1.getInt("delivery_address_id");
					refund_payment_status = srs_order_cancel1.getString("payment_status");

				}
				srs_order_cancel1.close();
				stmt1.close();

				String sql_order1 = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and product_id!=" + product_id1;
				logger.log("\n sql_order_product_unic" + sql_order1);
				Statement stmt_cu = conn.createStatement();
				ResultSet cust_ord = stmt_cu.executeQuery(sql_order1);

				int cs = 0, totalproductinorder = 0;

				while (cust_ord.next()) {

					totalproductinorder++;
					String status = cust_ord.getString("delivery_status_code");

					if (status.equalsIgnoreCase("return_request") || status.equalsIgnoreCase("cancel")
							|| status.equalsIgnoreCase("return")||status.equalsIgnoreCase("Exchange") )  {

						cs++;
					}

				}
				String sql_order_update1 = null;
				if (totalproductinorder == cs) {

					sql_order_update1 = "UPDATE customer_orders SET order_status_code='return_request' WHERE order_id='"
							+ order_id + "'";
					logger.log("\n sql_order_update \n" + sql_order_update1);
					Statement stmt_order_del1 = conn.createStatement();
					stmt_order_del1.executeUpdate(sql_order_update1);
					stmt_order_del1.close();

				}
				cust_ord.close();
				stmt_cu.close();

				String sql_order_product = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and product_id='" + product_id1 + "'";

				Statement stmt_cust1 = conn.createStatement();
				ResultSet cust_order_product1 = stmt_cust1.executeQuery(sql_order_product);

				// logger.log("\nsql_cust_order_product \n" + sql_order_product);

				if (cust_order_product1.next() == false) {

					Str_msg = "product is not presant in this order";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

				} else {

					if (cust_order_product1.getString("delivery_status_code").equalsIgnoreCase("delivered")) {

						product_id = cust_order_product1.getInt("product_id");
						vendor_id1 = cust_order_product1.getInt("vendor_id");
						quantity = cust_order_product1.getInt("quantity");
						sale_price = cust_order_product1.getFloat("price") * quantity;
						order_product_vendor_id = cust_order_product1.getInt("vendor_id");
						shipping_charge = cust_order_product1.getFloat("shipping_charge");
						effective_sale_price = cust_order_product1.getFloat("effective_price");
						discount_product = cust_order_product1.getFloat("discounts");
						Date d=cust_order_product1.getDate("expected_date_of_delivery");
						
					
						
						
						SimpleDateFormat f1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
						String s1 = null;
						java.util.Date d2;
						java.util.Date d3 = null;

						Date dateStop = cust_order_product1.getDate("expected_date_of_delivery");
						String current = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());		
						d3 = f1.parse(current);
						long diff = d3.getTime()-dateStop.getTime() ;
						long diffDays = diff / (24 * 60 * 60 * 1000);
							

								if(diffDays>=10) {
									

									Str_msg = "Product cannot be returned as the duration mentioned in return policy has expired.";
									jsonObject_cancelorder_result.put("status", "0");
									jsonObject_cancelorder_result.put("message", Str_msg);
									return jsonObject_cancelorder_result;	
									
								}
												
						
						
                      
					

					} else if (cust_order_product1.getString("delivery_status_code")
							.equalsIgnoreCase("return_request")) {

						Str_msg = "Request to return the product has already being taken.";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					} else if (cust_order_product1.getString("delivery_status_code").equalsIgnoreCase("Return")) {

						Str_msg = "Product already returned.";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					} else {

						Str_msg = "Product is yet to be delivered.";
						jsonObject_cancelorder_result.put("status", "0");
						jsonObject_cancelorder_result.put("message", Str_msg);
						return jsonObject_cancelorder_result;

					}
					cust_order_product1.close();
					stmt_cust1.close();

					Statement stmt_refund_insert1 = conn.createStatement();
					String sql_refund_insert1 = "insert into return_table(customer_id,order_id,product_id,address_id,refund_id,return_status,return_option,return_condition,order_return_reason,pickup_date)values"
							+ "('" + userid + "','" + order_id + "','" + product_id + "','" + shipping_address_id
							+ "','-1','" + refund_status + "','"+order_return_option+"','" + order_return_condition + "','"
							+ order_return_reason + "','" + schedule_pickup + "')";

					logger.log("\n stmt_return_insert \n" + sql_refund_insert1);
					int insert_add1 = stmt_refund_insert1.executeUpdate(sql_refund_insert1);
					stmt_refund_insert1.close();

					String Sql_order_return = "select * from return_table where order_id='" + order_id
							+ "' and product_id='" + product_id + "'";
					logger.log("\n Sql_order_cancel \n" + Sql_order_return);
					Statement stmt_return_table = conn.createStatement();
					ResultSet srs_order_return = stmt_return_table.executeQuery(Sql_order_return);

					int return_id_1;
					if (srs_order_return.next()) {
						return_id_1 = srs_order_return.getInt("return_id");

						Str_msg = "Order return request_id : " + return_id_1 + " generated Successfully! Thank You !! ";

					}
					srs_order_return.close();
					stmt_return_table.close();

					String sql_update_order_detail = "update customer_order_details set delivery_status_code='return_request' where order_number='"
							+ order_number + "' and product_id='" + product_id1 + "'";

					logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);

					Statement stmt_return_req_product = conn.createStatement();
					stmt_return_req_product.executeUpdate(sql_update_order_detail);
					stmt_return_req_product.close();

				}
			}

			else {
				// ----this section for complate order
				// Return----------------------------------------------------

				logger.log("\n flag :" + flag + "\n");

				String sql_order_product1 = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and delivery_status_code ='shipped' limit 1";

				Statement stmt_cust1 = conn.createStatement();
				ResultSet cust_order_product1 = stmt_cust1.executeQuery(sql_order_product1);

				if (cust_order_product1.next()) {

					Str_msg = "some part of order's products is/are not delivered so you can't return complete order";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

				}
				// int product_id=cust_order_product1.getInt("product_id");
				
				

				
				cust_order_product1.close();
				stmt_cust1.close();

				String sql_return_product1 = "SELECT * FROM customer_order_details where order_id='" + order_id
						+ "' and delivery_status_code ='delivered'";

				Statement stmt_product_array = conn.createStatement();
				ResultSet cust_return_product1 = stmt_product_array.executeQuery(sql_return_product1);

				StringBuilder str_product_ids = new StringBuilder();
				Date dateStop = null;
				while (cust_return_product1.next()) {
					
					
					
					dateStop =cust_return_product1.getDate("expected_date_of_delivery");
					
					
					
					SimpleDateFormat f1 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
					String s1 = null;
					java.util.Date d2;
					java.util.Date d3 = null;

					
					String current = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());		
					d3 = f1.parse(current);
					long diff = d3.getTime()-dateStop.getTime() ;
					long diffDays = diff / (24 * 60 * 60 * 1000);
						
					logger.log("\n"+diffDays);
					
							if(diffDays>=10) {
								
								Str_msg = "One/more products in your order cannot be returned as the duration mentioned in return policy has expired. Please try returning products individually, the ones inside duration mentioned in return policy.";
								jsonObject_cancelorder_result.put("status", "0");
								jsonObject_cancelorder_result.put("message", Str_msg);
								return jsonObject_cancelorder_result;	
								
							}

					
					
					int productid_order = cust_return_product1.getInt("product_id");
					str_product_ids.append(productid_order);
					str_product_ids.append(",");

				}
				
				
								
				

				int l = str_product_ids.length() - 1;
				String str_product_ids1 = str_product_ids.substring(0, l).toString();

				cust_return_product1.close();
				stmt_product_array.close();

				String Sql_order_cancel = "select * from customer_orders where order_id='" + order_id + "'";
				// logger.log("\n Sql_order_cancel \n" + Sql_order_cancel);
				Statement stmt = conn.createStatement();
				ResultSet srs_order_cancel = stmt.executeQuery(Sql_order_cancel);

				if (srs_order_cancel.next() == false) {

					Str_msg = "order is not present";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
				} else if (srs_order_cancel.getString("order_status_code").equalsIgnoreCase("cancel")) {

					Str_msg = "order already canceled ";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

				} else if (srs_order_cancel.getString("order_status_code").equalsIgnoreCase("return")||srs_order_cancel.getString("order_status_code").equalsIgnoreCase("Exchange")) {
					Str_msg = "order already return / Exchange so can't return request generate for this order";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;
				} else if (srs_order_cancel.getString("order_status_code").equalsIgnoreCase("delivered")) {

					Str_msg = "Your Order return request accepteble.";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);

					order_number = srs_order_cancel.getString("order_number");
					refund_transaction_id = srs_order_cancel.getString("transaction_id");
					refund_payment_status = srs_order_cancel.getString("payment_status");
					refund_mode_of_payment = srs_order_cancel.getString("mode_of_payment");
					refund_date_of_order_paid = srs_order_cancel.getString("date_of_order_paid");
					shipping_address_id = srs_order_cancel.getInt("delivery_address_id");
					refund_sale_price = srs_order_cancel.getFloat("sub_total");
					refund_grant_total = srs_order_cancel.getFloat("grand_total");
					refund_shipping_charge = srs_order_cancel.getFloat("shipping");
					srs_order_cancel.getFloat("tax");
					refund_shipping_discounts = srs_order_cancel.getFloat("discounts");

					if (refund_payment_status.equalsIgnoreCase("Done")) {

						Statement stmt_refund_insert = conn.createStatement();
						sql_refund_insert = "insert into return_table(customer_id,order_id,product_id,address_id,refund_id,return_status,return_option,return_condition,order_return_reason,pickup_date)values"
								+ "('" + userid + "','" + order_id + "','" + str_product_ids1 + "','"
								+ shipping_address_id + "','-1','" + refund_status + "','"+order_return_option+"','"
								+ order_return_condition + "','" + order_return_reason + "','" + schedule_pickup + "')";

						logger.log("\n stmt_return_insert \n" + sql_refund_insert);
						int insert_add = stmt_refund_insert.executeUpdate(sql_refund_insert);
						stmt_refund_insert.close();

					} else {

					}

				}
				else {
					Str_msg = "order was not still delivered so can't return request generate for this order";
					jsonObject_cancelorder_result.put("status", "0");
					jsonObject_cancelorder_result.put("message", Str_msg);
					return jsonObject_cancelorder_result;

					
					
					
				}
				srs_order_cancel.close();
				stmt.close();

				String Sql_order_return = "SELECT * FROM return_table WHERE order_id='" + order_id + "' ORDER BY return_id DESC LIMIT 1";
				
				logger.log("\n Sql_order_cancel \n" + Sql_order_return);
				Statement stmt_return_table = conn.createStatement();
				ResultSet srs_order_return = stmt_return_table.executeQuery(Sql_order_return);

				int return_id_1;
				if (srs_order_return.next()) {
					return_id_1 = srs_order_return.getInt("return_id");

					Str_msg = "Order return request_id : " + return_id_1 + " generated Successfully! Thank You !! ";

				}
				srs_order_return.close();
				stmt_return_table.close();

				String sql_update_order_detail = "update customer_order_details set delivery_status_code='return_request' where order_number='"
						+ order_number + "' and product_id in (" + str_product_ids1 + ")";

				logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);

				Statement stmt_return_req_product = conn.createStatement();
				stmt_return_req_product.executeUpdate(sql_update_order_detail);
				stmt_return_req_product.close();

				String sql_update_order_ = "update customer_orders set order_status_code='return_request' where order_number='"
						+ order_number + "'";

				logger.log("\n sql_cust_order_product_del \n" + sql_update_order_);

				Statement stmt_return_req_order = conn.createStatement();
				stmt_return_req_order.executeUpdate(sql_update_order_);
				stmt_return_req_order.close();

			}
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
		jsonObject_cancelorder_result.put("message", Str_msg);
		return jsonObject_cancelorder_result;

	}

}

/*
 * // Update product quantity when entire order cancel
 * 
 * String sql_order_product =
 * "SELECT * FROM customer_order_details where order_id='" + order_id +
 * "' and delivery_status_code!='cancel'";
 * 
 * //logger.log("\n sql_order_product \n" + sql_order_product);
 * 
 * Statement stmt_cust_order = conn.createStatement(); ResultSet
 * cust_order_products = stmt_cust_order.executeQuery(sql_order_product);
 * 
 * StringBuilder str_product_id = new StringBuilder();
 * 
 * while (cust_order_products.next()) {
 * 
 * int productid_order = cust_order_products.getInt("product_id");
 * 
 * str_product_id.append(productid_order); str_product_id.append(",");
 * 
 * int quantity1 = cust_order_products.getInt("quantity");
 * 
 * sql_pro = "update products set quantity=quantity + " + quantity1 +
 * " , stock='true' where id=" + productid_order +
 * " and vendor_id not in (SELECT id FROM admin WHERE is_external=1)";
 * 
 * //logger.log("\n sql_pro \n" + sql_pro); Statement stmt_order_product_update
 * = conn.createStatement(); stmt_order_product_update.executeUpdate(sql_pro);
 * stmt_order_product_update.close();
 * 
 * }
 * 
 * String refund_products_id = str_product_id.toString().substring(0,
 * str_product_id.toString().length() - 1); //logger.log("\n String Array : " +
 * refund_products_id);
 * 
 * cust_order_products.close(); stmt_cust_order.close();
 * 
 * sql_order_update =
 * "UPDATE customer_orders SET order_status_code='cancel'  WHERE order_id='" +
 * order_id + "'";
 * 
 * //logger.log("\n sql_order_update \n" + sql_order_update); Statement
 * stmt_order_del = conn.createStatement();
 * stmt_order_del.executeUpdate(sql_order_update); stmt_order_del.close();
 * 
 * // Cancel from tracking table
 * 
 * String sql_ordertraking_update =
 * "UPDATE order_tracking SET status='cancel',expected_date_of_delivery=NULL  WHERE order_id='"
 * + order_id + "'";
 * 
 * //logger.log("\n sql_ordertraking_update \n" + sql_ordertraking_update);
 * Statement stmt_ordertracking_del = conn.createStatement();
 * stmt_ordertracking_del.executeUpdate(sql_ordertraking_update);
 * stmt_ordertracking_del.close();
 * 
 * 
 * 
 * String sql_order_product_cancel1 =
 * "SELECT * FROM customer_order_details where order_id='" + order_id +
 * "' and delivery_status_code='cancel'";
 * 
 * Statement stmt_cust_cancel1 = conn.createStatement(); ResultSet
 * cust_order_product_cancel1 =
 * stmt_cust_cancel1.executeQuery(sql_order_product_cancel1);
 * 
 * float sale_price_total = 0, discount_add = 0, shipping_total = 0; int flag_1
 * = 0; while (cust_order_product_cancel1.next()) { flag_1 = 1;
 * 
 * int product_id_refund = cust_order_product_cancel1.getInt("product_id");
 * float sale_price_ref = cust_order_product_cancel1.getFloat("price"); int
 * product_quantity = cust_order_product_cancel1.getInt("quantity");
 * 
 * float effective = cust_order_product_cancel1.getFloat("effective_price");
 * float product_discount = cust_order_product_cancel1.getFloat("discounts");
 * 
 * sale_price_total = sale_price_ref * product_quantity;
 * 
 * String sql_order_product_refund = "SELECT * FROM refund where order_id='" +
 * order_id + "' and product_id='" + product_id_refund + "'";
 * 
 * Statement stmt_cust_refund = conn.createStatement(); ResultSet
 * cust_order_product_refund =
 * stmt_cust_refund.executeQuery(sql_order_product_refund);
 * 
 * if (cust_order_product_refund.next()) {
 * 
 * float refunf_grant_total =
 * cust_order_product_refund.getFloat("refund_grant_total"); // float shipping
 * =cust_order_product_refund.getFloat("shipping"); //
 * discount_add=discount_add+product_wise_discount; // float
 * product_wise_discount =cust_order_product_refund.getFloat("discounts"); //
 * shipping_charge=shipping_charge+shipping;
 * 
 * //logger.log("refund_grant_total" + refunf_grant_total); //
 * logger.log("refund_grant_total" + discount_add); //
 * logger.log("refund_grant_total" + shipping_charge);
 * 
 * if (refunf_grant_total - effective == 0) {
 * 
 * shipping_total = 0;
 * 
 * } else {
 * 
 * shipping_total = refunf_grant_total - effective;
 * 
 * }
 * 
 * //logger.log("\n shipping_total \n " + shipping_total);
 * 
 * //logger.log("\n shipping_charge \n " + shipping_charge);
 * 
 * } cust_order_product_refund.close(); stmt_cust_refund.close();
 * 
 * String sql_order_update1 =
 * "UPDATE customer_orders SET  sub_total=sub_total + " + sale_price_total +
 * " , grand_total=grand_total+ " + effective + "+" + shipping_total +
 * ",shipping=shipping+" + shipping_total + ",discounts=discounts +" +
 * product_discount + " WHERE order_id='" + order_id + "'";
 * 
 * //logger.log("\n sql_order_update \n" + sql_order_update1); Statement
 * stmt_order_del1 = conn.createStatement();
 * stmt_order_del1.executeUpdate(sql_order_update1); stmt_order_del1.close();
 * 
 * } cust_order_product_cancel1.close(); stmt_cust_cancel1.close();
 * 
 * // Update customer_order_details_table when entire order cancel
 * 
 * String sql_update_order_detail =
 * "update customer_order_details set delivery_status_code='cancel',expected_date_of_delivery=NULL where order_id='"
 * + order_id + "'";
 * 
 * //logger.log("\n sql_cust_order_product_del \n" + sql_update_order_detail);
 * 
 * Statement stmt_cust_del = conn.createStatement();
 * stmt_cust_del.executeUpdate(sql_update_order_detail); stmt_cust_del.close();
 * 
 * // Update customer_order_table when entire order cancel
 */
