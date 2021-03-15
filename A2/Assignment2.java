/* 
 * This code is provided solely for the personal and private use of students 
 * taking the CSC343H course at the University of Toronto. Copying for purposes 
 * other than this use is expressly prohibited. All forms of distribution of 
 * this code, including but not limited to public repositories on GitHub, 
 * GitLab, Bitbucket, or any other online platform, whether as given or with 
 * any changes, are expressly prohibited. 
*/ 

import java.sql.*;
import java.util.Date;
import java.util.Arrays;
import java.util.List;

public class Assignment2 {
   /////////
   // DO NOT MODIFY THE VARIABLE NAMES BELOW.
   
   // A connection to the database
   Connection connection;

   // Can use if you wish: seat letters
   List<String> seatLetters = Arrays.asList("A", "B", "C", "D", "E", "F");

   Assignment2() throws SQLException {
      try {
         Class.forName("org.postgresql.Driver");
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      }
   }		

  /**
   * Connects and sets the search path.
   *
   * Establishes a connection to be used for this session, assigning it to
   * the instance variable 'connection'.  In addition, sets the search
   * path to 'air_travel, public'.
   *
   * @param  url       the url for the database
   * @param  username  the username to connect to the database
   * @param  password  the password to connect to the database
   * @return           true if connecting is successful, false otherwise
   */
   public boolean connectDB(String URL, String username, String password) {
      // Implement this method!
      try {
         String query;
         PreparedStatement ps;
         connection = DriverManager.getConnection(URL,username,password); 
         query = "SET search_path TO air_travel, public; ";
         ps = connection.prepareStatement(query);
         ps.executeUpdate();
      } catch (SQLException ex){
         return false;
      }
      return true;
   }

  /**
   * Closes the database connection.
   *
   * @return true if the closing was successful, false otherwise
   */
   public boolean disconnectDB() {
      // Implement this method!
      try {
         connection.close(); 
      } catch (SQLException ex){
         return false;
      }
      return true;
   }
   
   /* ======================= Airline-related methods ======================= */

   /**
    * Attempts to book a flight for a passenger in a particular seat class. 
    * Does so by inserting a row into the Booking table.
    *
    * Read handout for information on how seats are booked.
    * Returns false if seat can't be booked, or if passenger or flight cannot be found.
    *
    * 
    * @param  passID     id of the passenger
    * @param  flightID   id of the flight
    * @param  seatClass  the class of the seat (economy, business, or first) 
    * @return            true if the booking was successful, false otherwise. 
    */
   public boolean bookSeat(int passID, int flightID, String seatClass) {
      // Implement this method!
      // general skeleton:
      // statement to get get id, price
      // book from economy, to business to first class
      // A B C D E F seats
      // overbooking economy???? dang have to accept null seats? and then upgrade? but cant use the upgrade function
      try{

         PreparedStatement booking_statement = connection.prepareStatement(
            "SELECT flight.id, plane.capacity_economy as econ_capacity, " +
            "plane.capacity_business as business_capacity, "+ 
            "plane.capacity_first as firstclass_capacity " +
            "FROM flight, plane " +
            "WHERE flight.plane = plane.tail_number and flight.id = ?");

         // set int to get the input
         booking_statement.setInt(1, flightID);
         ResultSet flight_capacity = booking_statement.executeQuery();
         
         PreparedStatement flight_price = connection.prepareStatement("SELECT * FROM price WHERE flight_id = ?");
         flight_price.setInt(1, flightID);
         ResultSet available_flights = flight_price.executeQuery();
         
         PreparedStatement seat_and_class = connection.prepareStatement(
               "SELECT count(*) FROM booking " +
               "WHERE booking.flight_id = ? " +
               "and seat_class = ?::seat_class ");
         seat_and_class.setInt(1, flightID);
         seat_and_class.setString(2, seatClass);

         ResultSet already_booked = seat_and_class.executeQuery();

         // ok so now we can enter the search so long as:
         // there is flight capacity, and its not already booked
         // and ofc the given flight exists
         while(already_booked.next() && available_flights.next() && flight_capacity.next()){

            int price = available_flights.getInt(seatClass);
            
            // does order matter? economy, business, first?
            // dont think so but for sake of logic/ordering will just assume that avg booker
            // wants cheapest to more expensive?

            if(seatClass == "economy"){

               // have to check/account for  for overbooking (aka the -10)
               // equivalent to: 
               if(already_booked.getInt("count") - flight_capacity.getInt("econ_capacity") < 10){

                  PreparedStatement econ_booking = connection.prepareStatement(
                     "INSERT INTO booking " +
                     "VALUES((SELECT MAX(id) FROM booking)+1, " + 
                     " ?, ?, ?, ?, ?::seat_class, ?, ?)" );
                  econ_booking.setInt(1, passID);
                  econ_booking.setInt(2, flightID);
                  econ_booking.setTimestamp(3, getCurrentTimeStamp());
                  econ_booking.setInt(4, price);
                  econ_booking.setString(5, seatClass);
                  
                  // case where we do not need overbooking
                  if(flight_capacity.getInt("econ_capacity") - already_booked.getInt("count") > 0){

                     int economy_start = flight_capacity.getInt("firstclass_capacity")/6 + flight_capacity.getInt("business_capacity")/6 + 3;		
                     int max_row = economy_start + already_booked.getInt("count")/6;
                     int max_letter_num = already_booked.getInt("count") % 6;

                     char max_letter = 'A';
                     if(max_letter_num == 0){
                        max_row = max_row + 1;
                     }else if (max_letter_num == 1){
                        max_letter = 'B'; 
                     }else if (max_letter_num == 2){
                        max_letter = 'C';
                     }else if (max_letter_num == 3){
                        max_letter = 'D';
                     }else if (max_letter_num == 4){
                        max_letter = 'E';
                     }else if (max_letter_num == 5){
                        max_letter = 'F';
                     }
            
                     econ_booking.setString(7, max_letter+" ");
                     econ_booking.setInt(6, max_row);
                     econ_booking.setString(7, String.valueOf(max_letter));
               
                  }else{
                     // the case where we have to set null bookings (aka dont assign seat row or numb, but create booking anyay)
                     econ_booking.setNull(6, Types.NULL);
                     econ_booking.setNull(7, Types.NULL);
                  }
                  // excuting our now complete statment
                  econ_booking.executeUpdate();

                  // printing as a sanity check
                  // PreparedStatement booking = connection.prepareStatement("SELECT id, pass_id, flight_id, seat_class, row, letter FROM booking");
                  // ResultSet rs = booking.executeQuery();
                  // while (rs.next()) {
                  //    int id = rs.getInt("id");
                  //    int pass_id = rs.getInt("pass_id");
                  //    int flight_id = rs.getInt("flight_id");
                  //    String seat_class = rs.getString("seat_class");
                  //    int row = rs.getInt("row");
                  //    String letter = rs.getString("letter");

                  //    System.out.println("id -> " + id + " pass_id -> " + pass_id + " flight_id -> " + flight_id + " seat_class -> " + seat_class + " row -> " + row + " letter -> " + letter);
                     
                  // }
                  // System.out.println("================================================================================================================");
                     
                   return true;

               }

               return false;
            } 
            // business class rules
            
            if(seatClass == "business"){ 
               // WE DONT CARE ABOUT OVERBOOKING HERE

               if(flight_capacity.getInt("business_capacity") - already_booked.getInt("count")> 0){

                  PreparedStatement business_booking = connection.prepareStatement(
                     "INSERT INTO booking " +
                     "VALUES((SELECT MAX(id) FROM booking)+1, " +
                     "?, ?, ?, ?, ?::seat_class, ?, ?)" );

                  business_booking.setInt(1, passID);
                  business_booking.setInt(2, flightID);
                  business_booking.setTimestamp(3, getCurrentTimeStamp());
                  business_booking.setInt(4, price);
                  business_booking.setString(5, seatClass);

                  int business_start = flight_capacity.getInt("firstclass_capacity")/6 + 2;

                  // another way to get first row, different from update function
                  int max_row = business_start + already_booked.getInt("count")/6;
                  // first letter
                  int max_letter_num = already_booked.getInt("count") % 6;

                  // could put in helper function but nahhhh
                  char max_letter = 'A';
                  if(max_letter_num == 0){
                     max_row = max_row + 1;
                  }else if (max_letter_num == 1){
                     max_letter = 'B';
                  }else if (max_letter_num == 2){
                     max_letter = 'C';
                  }else if (max_letter_num == 3){
                     max_letter = 'D';
                  }else if (max_letter_num == 4){
                     max_letter = 'E';
                  }else if (max_letter_num == 5){
                     max_letter = 'F';
                  }

                  business_booking.setInt(6, max_row);
                  business_booking.setString(7, max_letter+" ");
                  business_booking.setString(7, String.valueOf(max_letter));

                  // update business, no need to worry about allowing overbooking here
                  business_booking.executeUpdate();

                  //print result
                  // PreparedStatement booking = connection.prepareStatement("SELECT id, pass_id, flight_id, seat_class, row, letter FROM booking");
                  // ResultSet rs = booking.executeQuery();
                  // while (rs.next()) {
                  //    int id = rs.getInt("id");
                  //    int pass_id = rs.getInt("pass_id");
                  //    int flight_id = rs.getInt("flight_id");
                  //    String seat_class = rs.getString("seat_class");
                  //    int row = rs.getInt("row");
                  //    String letter = rs.getString("letter");

                  //    System.out.println("id -> " + id + " pass_id -> " + pass_id + " flight_id -> " + flight_id + " seat_class -> " + seat_class + " row -> " + row + " letter -> " + letter);
                     
                  // }
                  // System.out.println("================================================================================================================");
                     
                  return true;

               }
               return false;
            } 
            
            if(seatClass == "first"){

               if(flight_capacity.getInt("firstclass_capacity") - already_booked.getInt("count")> 0){
                  
                  PreparedStatement first_booking = connection.prepareStatement(
                     "INSERT INTO booking " +
                     "VALUES((SELECT MAX(id) FROM booking)+1, " +
                     "?, ?, ?, ?, ?::seat_class, ?, ?)" );

                  //remember to keep it in the same order as the og statment
                  first_booking.setInt(1, passID);
                  first_booking.setInt(2, flightID);
                  first_booking.setTimestamp(3, getCurrentTimeStamp());
                  first_booking.setInt(4, price);
                  first_booking.setString(5, seatClass);
                  int first_start = 1;		
                  int max_row = first_start + already_booked.getInt("count")/6;
                  
                  int max_letter_num = already_booked.getInt("count") % 6;

                  char max_letter = 'A';
                  if(max_letter_num == 0){
                     max_row = max_row + 1;
                     max_letter = 'A';
                  }else if (max_letter_num == 1){
                     max_letter = 'B'; 
                  }else if (max_letter_num == 2){
                     max_letter = 'C';
                  }else if (max_letter_num == 3){
                     max_letter = 'D';
                  }else if (max_letter_num == 4){
                     max_letter = 'E';
                  }else if (max_letter_num == 5){
                     max_letter = 'F';
                  }

                  first_booking.setInt(6, max_row);
                  first_booking.setString(7, max_letter+" ");
                  first_booking.setString(7, String.valueOf(max_letter));
                  first_booking.executeUpdate();
                 
                  // // printing
                  //PreparedStatement booking = connection.prepareStatement("SELECT id, pass_id, flight_id, seat_class, row, letter FROM booking");
                  // ResultSet rs = booking.executeQuery();
                  // while (rs.next()) {
                  //    int id = rs.getInt("id");
                  //    int pass_id = rs.getInt("pass_id");
                  //    int flight_id = rs.getInt("flight_id");
                  //    String seat_class = rs.getString("seat_class");
                  //    int row = rs.getInt("row");
                  //    String letter = rs.getString("letter");

                  //    System.out.println("id -> " + id + " pass_id -> " + pass_id + " flight_id -> " + flight_id + " seat_class -> " + seat_class + " row -> " + row + " letter -> " + letter);
                     
                  // }
                  // System.out.println("================================================================================================================");
                     
                  return true;

               }
               
               return false;
               
            } 
         }
      } catch(SQLException se){
		se.printStackTrace();
		return false;
   }
   // if we somehow get here
   return false;
   }

   /**
    * Attempts to upgrade overbooked economy passengers to business class
    * or first class (in that order until each seat class is filled).
    * Does so by altering the database records for the bookings such that the
    * seat and seat_class are updated if an upgrade can be processed.
    *
    * Upgrades should happen in order of earliest booking timestamp first.
    *
    * If economy passengers are left over without a seat (i.e. more than 10 overbooked passengers or not enough higher class seats), 
    * remove their bookings from the database.
    * 
    * @param  flightID  The flight to upgrade passengers in.
    * @return           the number of passengers upgraded, or -1 if an error occured.
    */
   public int upgrade(int flightID) {
      // Implement this method!
		try {

         // does this damn flight even EXIST
         String yeet =  "SELECT * FROM flight WHERE id=?";
         PreparedStatement yeet_statement = connection.prepareStatement(yeet);
         yeet_statement.setInt(1, flightID);
         ResultSet yeetexists = yeet_statement.executeQuery();

         if (!yeetexists.next()){
            //System.out.println("flight not found");
            return -1;	
         }

         // now we can check the overbooking
			String overbooked_query = "SELECT pass_id AS overbookedPassengers, id as book_id FROM booking " + 
                              "WHERE flight_id=? AND row IS NULL ORDER BY datetime";
	 		PreparedStatement overbooked_statement = connection.prepareStatement(overbooked_query);
         
         overbooked_statement.setInt(1, flightID);
         ResultSet overbookings = overbooked_statement.executeQuery();

	 		int total_upgrades = 0;
	 		int overbooked_passenger;
         int book_id;
					 
			//rs should now hold all overbooked passengers
			while (overbookings.next()){
				overbooked_passenger = overbookings.getInt("overbookedPassengers");
				book_id = overbookings.getInt("book_id");

            int currentBookedBusinessSeats = 0; // = currentSeatClassOccupation(flightID, "business") 
            String currently_booked_business_q = "SELECT count(*) AS occupancy_count FROM booking WHERE flight_id=? AND seat_class=?::seat_class";
            PreparedStatement cbbs = connection.prepareStatement(currently_booked_business_q);
            cbbs.setInt(1, flightID);
            cbbs.setString(2, "business");       
            ResultSet currentlyBookedBusiness = cbbs.executeQuery();
                     
            if (currentlyBookedBusiness.next()){
               currentBookedBusinessSeats=  currentlyBookedBusiness.getInt("occupancy_count");
            }else {
               //System.out.println("error accessing currently booked business seats");
               return -1;					 
            }
            //System.out.println("Currently booked business class seats: "+ currentBookedBusinessSeats);


            int totalBusinessClassCapacity =0; // = getSeatClassCapacity( flightID, "business")
            String business_cap_q = "SELECT capacity_business as business_capacity FROM flight, plane " + 
                                    "WHERE flight.id=? AND flight.plane = plane.tail_number";
            PreparedStatement bcp = connection.prepareStatement(business_cap_q);
           
            bcp.setInt(1, flightID);
            ResultSet business_cap_total = bcp.executeQuery();
         
            if (business_cap_total.next()){
               totalBusinessClassCapacity = business_cap_total.getInt("business_capacity");
            } else {
               return -1;
               //System.out.println("error in business class capacity");					 
            }
            //System.out.println("actual business capacity = " + totalBusinessClassCapacity);

            int totalfirstCapacity =0; // = getSeatClassCapacity( flightID, "first")
            String first_cap_q = "SELECT capacity_first as firstclass_capacity FROM flight, plane " + 
                                    "WHERE flight.id=? AND flight.plane = plane.tail_number";
            PreparedStatement fcp = connection.prepareStatement(first_cap_q);
           
            fcp.setInt(1, flightID);
            ResultSet first_cap_total = fcp.executeQuery();
         
            if (first_cap_total.next()){
               totalfirstCapacity = first_cap_total.getInt("firstclass_capacity");
            } else {
               return -1;
               //System.out.println("error in first class capacity");					 
            }
            //System.out.println("actual first class capacity = " + totalfirstCapacity);

            int currentBookedFirstClassSeats = 0; // = currentSeatClassOccupation(flightID, "first") 
            String currently_booked_first_q = "SELECT count(*) AS occupancy_count FROM booking WHERE flight_id=? AND seat_class=?::seat_class";
            PreparedStatement cbfs = connection.prepareStatement(currently_booked_first_q);
            cbfs.setInt(1, flightID);
            cbfs.setString(2, "first");       
            ResultSet currentlyBookedFirstClass = cbfs.executeQuery();
                     
            if (currentlyBookedFirstClass.next()){
               currentBookedFirstClassSeats =  currentlyBookedFirstClass.getInt("occupancy_count");
            } else {
               return -1;
               //System.out.println("error accessing currently booked first class seats");					 
            }
            //System.out.println("Currently booked first class seats: " + currentBookedFirstClassSeats);


            
				//See if we can upgrade them to business class
				//if (currentSeatClassOccupation(flightID, "business") < getSeatClassCapacity( flightID, "business")){
            if (currentBookedBusinessSeats < totalBusinessClassCapacity){
               String existance_query =  "SELECT * FROM booking WHERE flight_id=? and pass_id=?";

               PreparedStatement existance_statement = connection.prepareStatement(existance_query);
               // Insert that string into the PreparedStatement and execute it.
               existance_statement.setInt(2, overbooked_passenger);
               existance_statement.setInt(1, flightID);
               ResultSet exists = existance_statement.executeQuery();

			      if (!exists.next()){
				      //System.out.println("BOOKING NOT FOUND");
				      return -1;	
			      }
               // else continue
               // System.out.println("update business class");

               String update_econ_query = "UPDATE booking " + 
                                    "SET seat_class=?::seat_class, row=?, letter=? " + 
                                    "WHERE pass_id=? AND flight_id=? AND id =? ";
               PreparedStatement update_econ = connection.prepareStatement(update_econ_query);
               
               int firstRowOfBusiness, firstRowOfEcon;

               // getting business capacity
               int totalBusinessCapacity = 0;
               String business_cap_query = "SELECT capacity_business AS business_capacity FROM flight, plane " + 
                                          "WHERE flight.id=? AND flight.plane= plane.tail_number";
               PreparedStatement business_cap_statment = connection.prepareStatement(business_cap_query);
               business_cap_statment.setInt(1, flightID);
               ResultSet business_capacity = business_cap_statment.executeQuery();
			
               if (business_capacity.next()){
                  totalBusinessCapacity =  business_capacity.getInt("business_capacity");
               }
               else {
                  return -1;
                  //System.out.println("invalid seat capacity");					 
               }

               //getting first class capacity
               int totalFirstClassCapacity_wrt_business = 0;
               // WITH
               String first_cap_query_in_business = "SELECT capacity_first AS firstclass_capacity FROM flight, plane " + 
                                          "WHERE flight.id=? AND flight.plane= plane.tail_number";
               PreparedStatement first_cap_statement_in_business = connection.prepareStatement(first_cap_query_in_business);
               first_cap_statement_in_business.setInt(1, flightID);
               ResultSet firstclass_capacity_in_business = first_cap_statement_in_business.executeQuery();
			
               if (firstclass_capacity_in_business.next()){
                  totalFirstClassCapacity_wrt_business =  firstclass_capacity_in_business.getInt("firstclass_capacity");
               } else {
                  //System.out.println("invalid seat capacity");		
                  return -1;			 
               }

               int fullRowsUsedByFirst= totalFirstClassCapacity_wrt_business/6;
               int remainder= totalFirstClassCapacity_wrt_business % 6;
               if (remainder==0){
                  firstRowOfBusiness =  fullRowsUsedByFirst+1;
               }else{
                  firstRowOfBusiness =  fullRowsUsedByFirst+2;
               }

               int businessFirstRow= firstRowOfBusiness;
               int fullRowsUsedByBusiness= totalBusinessCapacity/6;
               int remainder_business= totalBusinessCapacity%6;
               if (remainder_business==0){
                  firstRowOfEcon = businessFirstRow + fullRowsUsedByBusiness;
               }else{
                  firstRowOfEcon = businessFirstRow + fullRowsUsedByBusiness + 1;
               }
               
               //String seat = "";
               int row = 0;
               String seatLetter = "";

               for (int i= firstRowOfBusiness; i < firstRowOfEcon;  i++)	{
                  for (int j=0; j< seatLetters.size() && totalBusinessCapacity!=0 ;j++) {
                     if (!alreadyBooked(flightID, i, seatLetters.get(j))) {
                        row = i;
                        //System.out.println(i);
                        seatLetter = seatLetters.get(j);
                        //System.out.println(seatLetters.get(j));
                     }
                     totalBusinessCapacity--;
                  }
               }

               update_econ.setString(1, "business");
               update_econ.setInt(2, row);
               update_econ.setString(3, seatLetter);
               update_econ.setInt(4, overbooked_passenger);
               update_econ.setInt(5, flightID);
               update_econ.setInt(6, book_id);
               update_econ.executeUpdate();
               total_upgrades++;

            }else if (currentBookedFirstClassSeats < totalfirstCapacity ){   
               String existance_query =  "SELECT * FROM booking WHERE flight_id=? and pass_id=?";
               PreparedStatement existance_statement = connection.prepareStatement(existance_query);
               // Insert that string into the PreparedStatement and execute it.
               existance_statement.setInt(2, overbooked_passenger);
               existance_statement.setInt(1, flightID);
               ResultSet exists = existance_statement.executeQuery();

               if (!exists.next()){
                  //System.out.println("BOOKING NOT FOUND");
                  return -1;	
			      }
               // else continue
               // System.out.println("update first class");

               String update_first_query = "UPDATE booking " + 
                                    "SET seat_class=?::seat_class, row=?, letter=? " + 
                                    "WHERE pass_id=? AND flight_id=? AND id =? "; 
               PreparedStatement update_first = connection.prepareStatement(update_first_query);
               //String seat = obtainFirstSeatUpgrade(flightID, "first");

               int firstRowOfFirst, firstRowOfBusiness;
		         //int capacityCounter= getSeatClassCapacity(flightID, "first");

               // getting first class capacity
               int totalFirstClassCapacity = 0;
               // WITH
               String first_cap_query = "SELECT capacity_first AS firstclass_capacity FROM flight, plane " + 
                                          "WHERE flight.id=? AND flight.plane= plane.tail_number";
               PreparedStatement first_cap_statement = connection.prepareStatement(first_cap_query);
               first_cap_statement.setInt(1, flightID);
               ResultSet firstclass_capacity = first_cap_statement.executeQuery();
			
               if (firstclass_capacity.next()){
                  totalFirstClassCapacity =  firstclass_capacity.getInt("firstclass_capacity");
               }
               else {
                  return -1;
                  // System.out.println("invalid seat capacity");					 
               }

               // Replaced
		         firstRowOfFirst= 1;
               int fullRowsUsedByFirst= totalFirstClassCapacity/6;
               int remainder= totalFirstClassCapacity % 6;
               if (remainder==0){
                  firstRowOfBusiness =  fullRowsUsedByFirst+1;
               }else{
                  firstRowOfBusiness =  fullRowsUsedByFirst+2;
               }


               int row = 0;
               String seatLetter = "";

               for (int i= firstRowOfFirst; i < firstRowOfBusiness;  i++)	{
                  for (int j=0; j < seatLetters.size() && totalFirstClassCapacity!=0 ;j++) {
                     if (!alreadyBooked(flightID, i, seatLetters.get(j))) {
                        row = i;
                        seatLetter = seatLetters.get(j);
                     }
                     totalFirstClassCapacity--;
                  }
               }

               update_first.setString(1, "first");
               update_first.setInt(2, row);
               update_first.setString(3, seatLetter);
               update_first.setInt(4, overbooked_passenger);
               update_first.setInt(5, flightID);
               update_first.setInt(6, book_id);
               update_first.executeUpdate();
					total_upgrades++;

				} else {

               //System.out.println("cannot accomodate, removing");
               removeOverBooking(overbooked_passenger, flightID, book_id);
			      // remove_booking.setInt(1, book_id);
		 	      // remove_booking.executeUpdate();
				}
			}
			return total_upgrades;
		}
		catch (SQLException se) {
			System.out.println("SQL EXCEPTION in upgrade");
         System.err.println("<Message>: " + se.getMessage());
	 		se.printStackTrace();
			return -1;		
		}
   }


   /* ----------------------- Helper functions below  ------------------------- */

    // A helpful function for adding a timestamp to new bookings.
    // Example of setting a timestamp in a PreparedStatement:
    // ps.setTimestamp(1, getCurrentTimeStamp());

    /**
    * Returns a SQL Timestamp object of the current time.
    * 
    * @return           Timestamp of current time.
    */
   private java.sql.Timestamp getCurrentTimeStamp() {
      java.util.Date now = new java.util.Date();
      return new java.sql.Timestamp(now.getTime());
   }

   // Add more helper functions below if desired.
   // two helper functions
   // for some reason, removing the the deletion into another function reduces errors

   public boolean removeOverBooking(int pass_id, int flightID, int bookID) {
      try {
         
         String booking_query = "SELECT * FROM booking WHERE pass_id=? and flight_id=?";
         PreparedStatement bq = connection.prepareStatement(booking_query);
         bq.setInt(1, pass_id);
         bq.setInt(2, flightID);

         ResultSet get_bookings = bq.executeQuery();
         if (!get_bookings.next()){
            //if there are no bookings return false, something is wrong
            return false;	
         }

         String delete_query = "DELETE FROM booking WHERE id =?";
         PreparedStatement deletion = connection.prepareStatement(delete_query);
         deletion.setInt(1, bookID);
         deletion.executeUpdate();

      }
         
      catch (SQLException se) {
         System.out.println("SQL EXCEPTION in removeOverBooking");
         se.printStackTrace();
         return false;		
      } 
      return true;
   }

   // return false if the seat is available
   public boolean alreadyBooked (int flightID, int row, String letter) {
      try {

         String occupancy_query = "SELECT * FROM booking WHERE row=? and flight_id=? and letter=?";
         PreparedStatement osq = connection.prepareStatement(occupancy_query);
         osq.setInt(1, row);
         osq.setInt(2, flightID);
         osq.setString(3, letter);
         ResultSet occupied= osq.executeQuery();
                  
         if (!occupied.next()){
            return false;	
         }else{
            return true;
         }
      }
      catch (SQLException se) {
         System.out.println("SQL EXCEPTION in alreadyBooked");
         se.printStackTrace();
         return false;		
      }
   }
  
  /* ----------------------- Main method below  ------------------------- */

   public static void main(String[] args) {
      // You can put testing code in here. It will not affect our autotester.
      System.out.println("Running the code!");
      try{
      Assignment2 a2 = new Assignment2();
      boolean con = a2.connectDB("jdbc:postgresql://localhost:5432/csc343h-abdall77", "abdall77", "");
      // boolean b= a2.bookSeat(1,1,"economy");
      // boolean c = a2.bookSeat(1,4,"first");
      // if (b){
      //    System.out.println("booked passenger 1 on flight 1 in class econ");
      // }
      // if (c){
      //    System.out.println("booked passenger 1 on flight 4 in class first");
      // }
      // //b= a2.bookSeat(1,10,"business");
      // //b = a2.bookSeat(1,6,"economy"); 
      // //b = a2.bookSeat(1,1,"first");
      // //b = a2.bookSeat(1,4,"business");   
      // int upgrade_num = a2.upgrade(10);
      // System.out.println(upgrade_num);

      // second set of test
      // a2.bookSeat(1, 5,  "economy");		
      // a2.bookSeat(2, 5,  "economy");
      // a2.bookSeat(3, 5,  "economy");
      // a2.bookSeat(4, 5,  "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(1, 10, "economy");
      // a2.bookSeat(2, 3, "economy");

      // a2.bookSeat(1, 7,  "economy");
      // a2.bookSeat(1, 7,  "economy");

	   
      // a2.bookSeat(1, 5,  "first");
      // int upgrade_num = a2.upgrade(5);
      // System.out.println(upgrade_num);
      // int upgrade_num2 = a2.upgrade(10);
      // System.out.println(upgrade_num2);
      // int upgrade_num3 = a2.upgrade(7);
      // System.out.println(upgrade_num3);
      // int upgrade_num4 = a2.upgrade(77);
      // System.out.println(upgrade_num4);

      // overbooking econ
      for (int i = 0; i < 130; i++){
         a2.bookSeat(1, 7,  "economy");
      }
      // completely booking business
      for (int i = 0; i <19; i++){
         a2.bookSeat(1, 7,  "business");
      }
      int upgrade_num = a2.upgrade(7);
      System.out.println(upgrade_num);

      //upgrading a flight that doesnt exist
      int upgrade_num4 = a2.upgrade(1);
      System.out.println(upgrade_num4);

      }catch(SQLException se){
         se.printStackTrace();	
      }
   }
}
