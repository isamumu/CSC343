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
            // check placement of the brackets
            // (flight_capacity.getInt("capacity_economy") - already_booked.getInt("count")) > -10)
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
                     max_letter = 'B'; //(char)(max_letter + max_letter_num);
                  }else if (max_letter_num == 2){
                     max_letter = 'C';//(char)(max_letter + max_letter_num);
                  }else if (max_letter_num == 3){
                     max_letter = 'D';//(char)(max_letter + max_letter_num);
                  }else if (max_letter_num == 4){
                     max_letter = 'E';//(char)(max_letter + max_letter_num);
                  }else if (max_letter_num == 5){
                     max_letter = 'F';//(char)(max_letter + max_letter_num);
                  }
         
                  econ_booking.setString(7, max_letter+" ");
                  econ_booking.setInt(6, max_row);
                  econ_booking.setString(7, String.valueOf(max_letter));
            
               }else{

                  econ_booking.setNull(6, Types.NULL);
                  econ_booking.setNull(7, Types.NULL);
               }
               
               econ_booking.executeUpdate();

               PreparedStatement booking = connection.prepareStatement("SELECT id, pass_id, flight_id, seat_class, row, letter FROM booking");


               ResultSet rs = booking.executeQuery();
               while (rs.next()) {
                  int id = rs.getInt("id");
                  int pass_id = rs.getInt("pass_id");
                  int flight_id = rs.getInt("flight_id");
                  String seat_class = rs.getString("seat_class");
                  int row = rs.getInt("row");
                  String letter = rs.getString("letter");

                  System.out.println("id -> " + id + " pass_id -> " + pass_id + " flight_id -> " + flight_id + " seat_class -> " + seat_class + " row -> " + row + " letter -> " + letter);
                  
               }
               System.out.println("================================================================================================================");
               
               return true;

            }
            return false;
         } 
         // business class rules
         
         if(seatClass == "business"){
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
               int max_row = business_start + already_booked.getInt("count")/6;
               
               int max_letter_num = already_booked.getInt("count") % 6;

               // could put in helper function but nahhhh
               char max_letter = 'A';
               if(max_letter_num == 0){
                  max_row = max_row + 1;
               }else if (max_letter_num == 1){
                  max_letter = 'B'; //(char)(max_letter + max_letter_num);
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
               business_booking.executeUpdate();

               PreparedStatement booking = connection.prepareStatement("SELECT id, pass_id, flight_id, seat_class, row, letter FROM booking");


               ResultSet rs = booking.executeQuery();
               while (rs.next()) {
                  int id = rs.getInt("id");
                  int pass_id = rs.getInt("pass_id");
                  int flight_id = rs.getInt("flight_id");
                  String seat_class = rs.getString("seat_class");
                  int row = rs.getInt("row");
                  String letter = rs.getString("letter");

                  System.out.println("id -> " + id + " pass_id -> " + pass_id + " flight_id -> " + flight_id + " seat_class -> " + seat_class + " row -> " + row + " letter -> " + letter);
                  
               }
               System.out.println("================================================================================================================");

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

               PreparedStatement booking = connection.prepareStatement("SELECT id, pass_id, flight_id, seat_class, row, letter FROM booking");

               ResultSet rs = booking.executeQuery();
               while (rs.next()) {
                  int id = rs.getInt("id");
                  int pass_id = rs.getInt("pass_id");
                  int flight_id = rs.getInt("flight_id");
                  String seat_class = rs.getString("seat_class");
                  int row = rs.getInt("row");
                  String letter = rs.getString("letter");

                  System.out.println("id -> " + id + " pass_id -> " + pass_id + " flight_id -> " + flight_id + " seat_class -> " + seat_class + " row -> " + row + " letter -> " + letter);

               }
               System.out.println("================================================================================================================");

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
      // from handout, should be business, first class, econ for upgrades
      // still only have 6 seats per row (hence the mod 6/ usage of chars)
      try{

         PreparedStatement business_statement = connection.prepareStatement(
            "SELECT flight_id, count(id), max(row) as max_row  " +
            "FROM booking " +
            "WHERE flight_id = ? and seat_class = 'business' " +
            "Group By (flight_id)");
         business_statement.setInt(1, flightID);
         ResultSet business_booking = business_statement.executeQuery();

         PreparedStatement first_statement = connection.prepareStatement(
            "SELECT flight_id, count(id), max(row) as max_row " +
            "FROM booking " +
            "WHERE flight_id = ? and seat_class = 'first' " +
            "Group By (flight_id)");
         first_statement.setInt(1, flightID);
         ResultSet firstclass_booking = first_statement.executeQuery();

         PreparedStatement economy_statement = connection.prepareStatement(
            "SELECT flight_id, count(id), max(row) as max_row  " +
            "FROM booking " +
            "WHERE flight_id = ? and seat_class = 'economy' " +
            "Group By (flight_id)");
         economy_statement.setInt(1, flightID);
         ResultSet economy_booking = economy_statement.executeQuery();

         PreparedStatement capacity_statement = connection.prepareStatement(
            "SELECT flight.id, plane.capacity_business as business_capacity, " +
            "plane.capacity_economy as economy_capacity, " + 
            "plane.capacity_first as firstclass_capacity " +
            "FROM flight, plane " +
            "WHERE flight.plane = plane.tail_number " +
            "and flight.id = ?");
         capacity_statement.setInt(1, flightID);
         ResultSet flight_capacity = capacity_statement.executeQuery();
         
         while(business_booking.next() && firstclass_booking.next() &&  economy_booking.next() && flight_capacity.next()){
            
            int max_business_upgrades = -business_booking.getInt("count")+flight_capacity.getInt("business_capacity");
            //println(max_business_upgrades);
            int max_firstclass_upgrades = -firstclass_booking.getInt("count")+flight_capacity.getInt("firstclass_capacity");
            //println(max_firstclass_upgrades);

            int business_upgrades = 0;
            int firstclass_upgrades = 0;
            int total_upgrades = 0;

            if(flight_capacity.getInt("economy_capacity") >= economy_booking.getInt("count")){
               // aka theres still space? Cant book?
               // no upgrades but no errors yeEET
               return 0;		
            } else {
               
               PreparedStatement overbooked_flights = connection.prepareStatement(
                  "SELECT id " +
                  "FROM booking " +
                  "WHERE flight_id = ? and seat_class = 'economy' and row is NULL and letter is NULL " +
                  "Order by datetime");
               overbooked_flights.setInt(1, flightID);
               ResultSet null_bookings = overbooked_flights.executeQuery();

               // while it is all overbooked
               while(null_bookings.next()){

                  if(max_business_upgrades > 0){
                     PreparedStatement modify = connection.prepareStatement(
                        "UPDATE booking " +
                        "SET seat_class = 'business' "+ 
                        "AND row = ? and letter = ? " +
                        "WHERE booking.id = null_bookings.id ");

                     int max_row = business_booking.getInt("max_row");
                     int max_letter_num = business_booking.getInt("count") % 6;
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

                     modify.setInt(1, max_row);
                     modify.setString(2, max_letter+" ");
                     modify.setString(2, String.valueOf(max_letter));
                     
                     modify.executeUpdate();	
                     
                     // aka if we repeat, we no longer have same upgrade capacity
                     business_upgrades = business_upgrades + 1;
                     max_business_upgrades = max_business_upgrades - 1;				
                  
                  } else if(max_firstclass_upgrades > 0){
                     PreparedStatement modify = connection.prepareStatement(
                     "update booking " +
                     "set seat_class = 'first' and row = ? and letter = ? " +
                     "WHERE booking.id = null_bookings.id ");

                     int max_row = firstclass_booking.getInt("max_row");
               
                     int max_letter_num = firstclass_booking.getInt("count") % 6;

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

                     modify.setInt(1, max_row);
                     modify.setString(2, max_letter+" ");
                     modify.setString(2, String.valueOf(max_letter));
                     
                     modify.executeUpdate();	
                     // aka if we repeat, we no longer have same upgrade capacity
                     firstclass_upgrades = firstclass_upgrades + 1;
                     max_firstclass_upgrades = max_firstclass_upgrades - 1;
                     
                  } else {
                     // the overbooked passenger loses their booking :(((
                     PreparedStatement delete_booking = connection.prepareStatement(
                     "Delete from booking WHERE booking.id = ? ");
                     // i think delete is the right command?
                     delete_booking.setInt(1, null_bookings.getInt("id"));
                     delete_booking.executeUpdate();				
                  }
               }
               // total upgrades
               total_upgrades = firstclass_upgrades + business_upgrades;
               return total_upgrades;
            }
         }

      } catch(SQLException se){
         se.printStackTrace();
         // return false; they want -1
         return -1;
      }
      //return false;
      return -1;
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

   // no extra helper functions atm
  
  /* ----------------------- Main method below  ------------------------- */

   public static void main(String[] args) {
      // You can put testing code in here. It will not affect our autotester.
      System.out.println("Running the code!");
      try{
      Assignment2 a2 = new Assignment2();
      boolean con = a2.connectDB("jdbc:postgresql://localhost:5432/csc343h-poyisamu", "poyisamu", "");
      System.out.println("successfully connected!");
      
      // for(int i = 1; i < 7; i++){
      //    boolean b= a2.bookSeat(i,10,"economy");
      // }
      //boolean b= a2.bookSeat(4,7,"business");
      System.out.println("successfully booked economy seat!");
      // b = a2.bookSeat(1,10,"business");
      // System.out.println("successfully booked business seat!");
      // b = a2.bookSeat(1,6,"economy"); 
      // System.out.println("successfully booked economy seat!");
      // b = a2.bookSeat(1,1,"first");
      // System.out.println("successfully booked first seat!");
      // b = a2.bookSeat(1,4,"business");  
      // System.out.println("successfully booked business seat!"); 
      int upgrade_num = a2.upgrade(5);
      //System.out.println(upgrade_num);
      boolean dis = a2.disconnectDB();
      System.out.println("successfully disconnected DB!!");
      }catch(SQLException se){
         se.printStackTrace();	
      }
   }

}
