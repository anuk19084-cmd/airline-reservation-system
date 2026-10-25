import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server =
                HttpServer.create(
                        new InetSocketAddress(8080), 0);
        Path projectFolder =
                Paths.get("..")
                        .toAbsolutePath()
                        .normalize();
        server.createContext("/", exchange -> {
            String path =
                    exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/register.html";
            }
            Path file = projectFolder.resolve(
                            path.substring(1))
                            .normalize();
            if (!Files.exists(file)
                    || Files.isDirectory(file)) {
                String response =
                        "404 - File Not Found";
                exchange.sendResponseHeaders(
                        404,
                        response.length());
                OutputStream output =
                        exchange.getResponseBody();
                output.write(response.getBytes());
                output.close();
                return;
            }
            byte[] data =
                    Files.readAllBytes(file);
            String contentType =
                    "text/html";
            if (path.endsWith(".css")) {
                contentType = "text/css";
            }
            if (path.endsWith(".js")) {
                contentType =
                        "application/javascript";
            }
            exchange.getResponseHeaders()
                    .set("Content-Type",
                            contentType);
            exchange.sendResponseHeaders(
                    200,
                    data.length);
            OutputStream output =
                    exchange.getResponseBody();
            output.write(data);
            output.close();
        });
server.createContext("/api/register", exchange -> {
try{
    if (!exchange.getRequestMethod()
            .equalsIgnoreCase("POST")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }
    String body =
            new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8);
    String[] parts = body.split("&");
    String name = "";
    String email = "";
    String userPassword = "";
    for (String part : parts) {
        String[] pair = part.split("=", 2);
        if (pair.length < 2) {
            continue;
        }
        String key =
                URLDecoder.decode(
                        pair[0],
                        StandardCharsets.UTF_8);
        String value =
                URLDecoder.decode(
                        pair[1],
                        StandardCharsets.UTF_8);
        if (key.equals("name")) {
            name = value;
        }
        if (key.equals("email")) {
            email = value;
        }
        if (key.equals("password")) {
            userPassword = value;
        }
    }
    String url =
            "jdbc:mysql://localhost:3306/airline_reservation";
    String username = System.getenv("DB_USERNAME");
    String password = System.getenv("DB_PASSWORD");
    Connection db =
            DriverManager.getConnection(
                    url,
                    username,
                    password);
    PreparedStatement stmt =
            db.prepareStatement(
                    "INSERT INTO users " +
                    "(name, email, password) " +
                    "VALUES (?, ?, ?)");
    stmt.setString(1, name);
    stmt.setString(2, email);
    stmt.setString(3, userPassword);
    stmt.executeUpdate();
    stmt.close();
    db.close();
    String response =
            "Registration successful!";
    exchange.sendResponseHeaders(
            200,
            response.getBytes().length);
    exchange.getResponseBody()
            .write(response.getBytes());
    exchange.close();
} catch (Exception e) {
    e.printStackTrace();
    String response = "Registration failed!";
    exchange.sendResponseHeaders(
            500,
            response.getBytes().length);
    exchange.getResponseBody()
            .write(response.getBytes());
    exchange.close();
}
});
// Login API
server.createContext("/api/login", exchange -> {
    try {
        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        String body =
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8);
        String[] parts = body.split("&");
        String email = "";
        String userPassword = "";
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length < 2) {
                continue;
            }
            String key =
                    URLDecoder.decode(
                            pair[0],
                            StandardCharsets.UTF_8);
            String value =
                    URLDecoder.decode(
                            pair[1],
                            StandardCharsets.UTF_8);
            if (key.equals("email")) {
                email = value;
            }
            if (key.equals("password")) {
                userPassword = value;
            }
        }
        String url =
                "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(
                        url, username, password);
        PreparedStatement stmt =
                db.prepareStatement(
                        "SELECT * FROM users " +
                        "WHERE email = ? AND password = ?");
        stmt.setString(1, email);
        stmt.setString(2, userPassword);
        var result = stmt.executeQuery();
        String response;
        if (result.next()) {
            response = "Login successful!";
        } else {
            response = "Invalid email or password!";
        }
        stmt.close();
        db.close();
        exchange.sendResponseHeaders(
                200, response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response = "Login failed!";
        exchange.sendResponseHeaders(
                500, response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
// Flight Search API
server.createContext("/api/flights", exchange -> {
    try {
        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        String url =
                "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(
                        url, username, password);
        PreparedStatement stmt =
                db.prepareStatement(
                        "SELECT * FROM flights");
        var result = stmt.executeQuery();
        StringBuilder json =
                new StringBuilder("[");
        while (result.next()) {
            json.append("{")
                .append("\"airline\":\"")
                .append(result.getString("airline"))
                .append("\",")
                .append("\"origin\":\"")
                .append(result.getString("origin"))
                .append("\",")
                .append("\"destination\":\"")
                .append(result.getString("destination"))
                .append("\",")
                .append("\"departure_time\":\"")
                .append(result.getString("departure_time"))
                .append("\",")
                .append("\"arrival_time\":\"")
                .append(result.getString("arrival_time"))
                .append("\",")
                .append("\"price\":")
                .append(result.getDouble("price"))
                .append("},");
        }
        if (json.charAt(json.length() - 1) == ',') {
            json.deleteCharAt(json.length() - 1);
        }
        json.append("]");
        stmt.close();
        db.close();
        String response = json.toString();
        exchange.getResponseHeaders()
                .set("Content-Type", "application/json");
        exchange.sendResponseHeaders(
                200, response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response = "Flight search failed!";
        exchange.sendResponseHeaders(
                500, response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
// Booking API
server.createContext("/api/bookings", exchange -> {
    try {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        String passengerName = "";
        String airline = "";
        String seat = "";
        String pnr = "";
        String paymentMethod = "";
        String[] parts = body.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length < 2) {
                continue;
            }
            String key = URLDecoder.decode(
                    pair[0],
                    StandardCharsets.UTF_8);
            String value = URLDecoder.decode(
                    pair[1],
                    StandardCharsets.UTF_8);
            if (key.equals("passengerName")) {
                passengerName = value;
            }
            if (key.equals("airline")) {
                airline = value;
            }
            if (key.equals("seat")) {
                seat = value;
            }
            if (key.equals("pnr")) {
                pnr = value;
            }
            if (key.equals("paymentMethod")) {
                paymentMethod = value;
            }
        }
        String url =
                "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(
                        url, username, password);
        PreparedStatement stmt =
                db.prepareStatement(
                        "INSERT INTO bookings " +
                        "(passenger_name, airline, seat, pnr, payment_method) " +
                        "VALUES (?, ?, ?, ?, ?)");
        stmt.setString(1, passengerName);
        stmt.setString(2, airline);
        stmt.setString(3, seat);
        stmt.setString(4, pnr);
        stmt.setString(5, paymentMethod);
        stmt.executeUpdate();
        stmt.close();
        db.close();
        String response = "Booking saved successfully!";
        exchange.sendResponseHeaders(
                200,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response = "Booking failed!";
        exchange.sendResponseHeaders(
                500,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
        System.out.println(
                "Starting server...");
        server.createContext("/api/mybookings", exchange -> {
    if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }
    try {
        String url = "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(url, username, password);
        Statement stmt = db.createStatement();
        ResultSet rs =
                stmt.executeQuery("SELECT * FROM bookings");
        StringBuilder json =
                new StringBuilder("[");
        boolean first = true;
        while (rs.next()) {
            if (!first) {
                json.append(",");
            }
            json.append("{");
            json.append("\"passenger_name\":\"")
                .append(rs.getString("passenger_name"))
                .append("\",");
            json.append("\"airline\":\"")
                .append(rs.getString("airline"))
                .append("\",");
            json.append("\"seat\":\"")
                .append(rs.getString("seat"))
                .append("\",");
            json.append("\"pnr\":\"")
                .append(rs.getString("pnr"))
                .append("\",");
            json.append("\"payment_method\":\"")
                .append(rs.getString("payment_method"))
                .append("\"");
            json.append("}");
            first = false;
        }
        json.append("]");
        rs.close();
        stmt.close();
        db.close();
        String response = json.toString();
        exchange.getResponseHeaders()
                .set("Content-Type", "application/json");
        exchange.sendResponseHeaders(
                200,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response = "[]";
        exchange.sendResponseHeaders(
                500,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
server.createContext("/api/cancel", exchange -> {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }
    try {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);

        String pnr = "";
        String[] parts = body.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length < 2) {
                continue;
            }
            String key = URLDecoder.decode(
                    pair[0],
                    StandardCharsets.UTF_8);
            String value = URLDecoder.decode(
                    pair[1],
                    StandardCharsets.UTF_8);
            if (key.equals("pnr")) {
                pnr = value;
            }
        }
        String url =
                "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(
                        url, username, password);
        PreparedStatement stmt =
                db.prepareStatement(
                        "DELETE FROM bookings WHERE pnr = ?");
        stmt.setString(1, pnr);
        int rows = stmt.executeUpdate();
        stmt.close();
        db.close();
        String response;
        if (rows > 0) {
            response = "Booking cancelled successfully!";
        } else {
            response = "PNR not found!";
        }
        exchange.sendResponseHeaders(
                200,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response = "Cancellation failed!";
        exchange.sendResponseHeaders(
                500,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
server.createContext("/api/deleteflight", exchange -> {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }
    try {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        String id = body.split("=")[1];
        String url =
                "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(
                        url, username, password);
        PreparedStatement stmt =
                db.prepareStatement(
                        "DELETE FROM flights WHERE id = ?");
        stmt.setInt(1, Integer.parseInt(id));
        int rows = stmt.executeUpdate();
        stmt.close();
        db.close();
        String response;
        if (rows > 0) {
            response = "Flight deleted successfully!";
        } else {
            response = "Flight ID not found!";
        }
        exchange.sendResponseHeaders(
                200,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response = "Delete failed!";
        exchange.sendResponseHeaders(
                500,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
server.createContext("/api/addflight", exchange -> {
    if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
        exchange.sendResponseHeaders(405, -1);
        exchange.close();
        return;
    }
    try {
        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
        String airline = "";
        String origin = "";
        String destination = "";
        String departure = "";
        String arrival = "";
        String price = "";
        String[] parts = body.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length < 2) {
                continue;
            }
            String key = URLDecoder.decode(
                    pair[0],
                    StandardCharsets.UTF_8);
            String value = URLDecoder.decode(
                    pair[1],
                    StandardCharsets.UTF_8);
            if (key.equals("airline")) {
                airline = value;
            }
            if (key.equals("origin")) {
                origin = value;
            }
            if (key.equals("destination")) {
                destination = value;
            }
            if (key.equals("departure")) {
                departure = value;
            }
            if (key.equals("arrival")) {
                arrival = value;
            }
            if (key.equals("price")) {
                price = value;
            }
        }
        String url =
                "jdbc:mysql://localhost:3306/airline_reservation";
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection db =
                DriverManager.getConnection(
                        url, username, password);
        PreparedStatement stmt =
                db.prepareStatement(
                        "INSERT INTO flights " +
                        "(airline, origin, destination, departure_time, arrival_time, price) " +
                        "VALUES (?, ?, ?, ?, ?, ?)");
        stmt.setString(1, airline);
        stmt.setString(2, origin);
        stmt.setString(3, destination);
        stmt.setString(4, departure);
        stmt.setString(5, arrival);
        stmt.setDouble(6, Double.parseDouble(price));
        stmt.executeUpdate();
        stmt.close();
        db.close();
        String response =
                "Flight added successfully!";
        exchange.sendResponseHeaders(
                200,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    } catch (Exception e) {
        e.printStackTrace();
        String response =
                "Add flight failed!";
        exchange.sendResponseHeaders(
                500,
                response.getBytes().length);
        exchange.getResponseBody()
                .write(response.getBytes());
        exchange.close();
    }
});
        server.start();
        System.out.println("Server Started!");
        System.out.println(
                "Open http://localhost:8080");
    }
}
