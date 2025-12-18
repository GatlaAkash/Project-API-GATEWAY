import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.*;

@SpringBootApplication
public class ApiSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSystemApplication.class, args);
    }

    // API GATEWAY ROUTING CONFIGURATION 
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth_path", r -> r.path("/auth/**").uri("http://localhost:8080"))
                .route("product_path", r -> r.path("/products/**").uri("http://localhost:8080"))
                .build();
    }

    // SECURITY UTILITY (JWT)
    private static final String SECRET = "MyInterViewSecretKey12345678901234567890";

    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
}

//  DATABASE ENTITY
@Entity
@Table(name = "users")
class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    public User() {}
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}

@Repository
interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}

//  AUTHENTICATION CONTROLLER 
@RestController
@RequestMapping("/auth")
class AuthController {
    private final UserRepository repo;
    public AuthController(UserRepository repo) { this.repo = repo; }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User loginReq) {
        User user = repo.findByUsername(loginReq.getUsername());
        if (user != null && user.getPassword().equals(loginReq.getPassword())) {
            String token = ApiSystemApplication.generateToken(user.getUsername());
            return Collections.singletonMap("token", token);
        }
        throw new RuntimeException("Unauthorized");
    }
}

// PROTECTED PRODUCT CONTROLLER
@RestController
class ProductController {
    @GetMapping("/products")
    public List<Map<String, Object>> getProducts(@RequestHeader("Authorization") String auth) {
        // Simple manual check for the interview demo
        if (auth == null || !auth.startsWith("Bearer ")) throw new RuntimeException("No Token");
        
        return List.of(
            Map.of("id", 1, "name", "Oracle Database License", "price", 5000),
            Map.of("id", 2, "name", "Java Cloud Service", "price", 200)
        );
    }
}