package com.auction.app.controllers.home;

import com.auction.app.MainController;
import com.auction.app.controllers.NavbarItemController;
import com.auction.app.controllers.UserSession;
import com.auction.app.domains.auth.auth.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.Map;

@Component
public class NavbarController {

    @FXML private HBox navDashboard;
    @FXML private HBox navExplore;
    @FXML private HBox navMarket;
    @FXML private HBox navAccount;
    @FXML private HBox navAdmin;
    @FXML private HBox navLogout;

    @FXML private NavbarItemController navDashboardController;
    @FXML private NavbarItemController navExploreController;
    @FXML private NavbarItemController navMarketController;
    @FXML private NavbarItemController navAccountController;
    @FXML private NavbarItemController navAdminController;
    @FXML private NavbarItemController navLogoutController;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MainController mainController;

    @FXML
    public void initialize() {
        configureNavLabels();
        evaluateSecurityRoleAccess();

        if (navDashboard != null) {
            navDashboard.setOnMouseClicked(event -> {
                System.out.println("Routing to main Dashboard HomeView workspace...");
                mainController.navigateTo("/ui/views/home/HomeView.fxml");
            });
        }

        if (navMarket != null) {
            navMarket.setOnMouseClicked(event -> {
                System.out.println("Routing to live standalone Market View frame wrapper...");
                // FIXED: Uses navigateTo directly to route to the unified views resource hierarchy
                mainController.navigateTo("/ui/views/market/MarketView.fxml");
            });
        }

        if (navAccount != null) {
            navAccount.setOnMouseClicked(event -> {
                System.out.println("Routing to dynamic sub-panel Account View framework...");
                mainController.navigateTo("/ui/views/account/AccountView.fxml");
            });
        }

        if (navAdmin != null) {
            navAdmin.setOnMouseClicked(event -> {
                System.out.println("Admin Portal requested. Loading metrics...");
            });
        }

        if (navLogout != null) {
            navLogout.setOnMouseClicked(event -> handleLogoutExecution());
        }
    }

    private void configureNavLabels() {
        if (navDashboardController != null) navDashboardController.setItemText("Dashboard");
        if (navExploreController != null) navExploreController.setItemText("Explore");
        if (navMarketController != null) navMarketController.setItemText("Market");
        if (navAccountController != null) navAccountController.setItemText("Account");
        if (navAdminController != null) navAdminController.setItemText("Admin Control");
        if (navLogoutController != null) navLogoutController.setItemText("Logout");
    }

    private void evaluateSecurityRoleAccess() {
        if (userSession == null || userSession.getUserDetails() == null) return;
        try {
            boolean isAdmin = userSession.getUserDetails().getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || auth.getAuthority().equals("ADMIN"));

            if (isAdmin && navAdmin != null) {
                navAdmin.setVisible(true);
                navAdmin.setManaged(true);
            }
        } catch (Exception ex) {
            System.out.println("Failed to evaluate user role permissions: " + ex.getMessage());
        }
    }

    private void handleLogoutExecution() {
        try {
            HttpServletRequest mockRequest = createMockLogoutRequest();
        } catch (Exception e) {
            System.out.println("Backend termination bypassed: " + e.getMessage());
        } finally {
            userSession.clearSession();
            SecurityContextHolder.clearContext();
            mainController.navigateTo("/ui/views/auth/LoginView.fxml");
        }
    }

    private HttpServletRequest createMockLogoutRequest() {
        Map<String, String> headers = Map.of(
                "User-Agent", "JavaFX Desktop Application Client",
                "Authorization", "Bearer " + (userSession.getAccessToken() != null ? userSession.getAccessToken() : "")
        );
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getHeader".equals(method.getName()) && args.length > 0) return headers.get((String) args[0]);
                    if ("getRemoteAddr".equals(method.getName())) return "127.0.0.1";
                    return null;
                }
        );
    }
}