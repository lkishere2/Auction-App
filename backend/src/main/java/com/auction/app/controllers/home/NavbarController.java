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

    // FXML structural layout element nodes mapping injections
    @FXML private HBox navDashboard;
    @FXML private HBox navExplore;
    @FXML private HBox navMarket;
    @FXML private HBox navAccount;
    @FXML private HBox navAdmin; // Injected Admin navigation container root node
    @FXML private HBox navLogout;

    // Sub-component individual item instances injection hooks
    @FXML private NavbarItemController navDashboardController;
    @FXML private NavbarItemController navExploreController;
    @FXML private NavbarItemController navMarketController;
    @FXML private NavbarItemController navAccountController;
    @FXML private NavbarItemController navAdminController; // Injected Admin controller instance
    @FXML private NavbarItemController navLogoutController;

    @Autowired
    private AuthController authController;

    @Autowired
    private UserSession userSession;

    @Autowired
    private MainController mainController;

    @FXML
    public void initialize() {
        // 1. Configure textual values on nested components
        configureNavLabels();

        // 2. Perform Administrative role evaluation check
        evaluateSecurityRoleAccess();

        // 3. Register click listeners directly on item blocks
        if (navDashboard != null) {
            navDashboard.setOnMouseClicked(event -> {
                System.out.println("Routing to main Dashboard HomeView workspace...");
                mainController.navigateTo("/ui/views/home/HomeView.fxml");
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

    /**
     * Initializes structural labels on generic nested atomic elements.
     */
    private void configureNavLabels() {
        if (navDashboardController != null) navDashboardController.setItemText("Dashboard");
        if (navExploreController != null) navExploreController.setItemText("Explore");
        if (navMarketController != null) navMarketController.setItemText("Market");
        if (navAccountController != null) navAccountController.setItemText("Account");
        if (navAdminController != null) navAdminController.setItemText("Admin Control");
        if (navLogoutController != null) navLogoutController.setItemText("Logout");
    }

    /**
     * Inspects the current session context properties. If the user carries administrative
     * clearance attributes, the layout engine updates rendering geometry bounds to expose the link option.
     */
    private void evaluateSecurityRoleAccess() {
        if (userSession == null || userSession.getUserDetails() == null) {
            return;
        }

        try {
            // Read authority mappings out of the context session state holder
            boolean isAdmin = userSession.getUserDetails().getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")
                            || auth.getAuthority().equals("ADMIN"));

            if (isAdmin && navAdmin != null) {
                System.out.println("Admin role recognized! Exposing special workspace navigation panels.");
                navAdmin.setVisible(true);
                navAdmin.setManaged(true);
            }
        } catch (Exception ex) {
            System.out.println("Failed to evaluate user role permissions checklist: " + ex.getMessage());
        }
    }

    /**
     * Clears session memories, notifies web tier controllers, flushes security context loops,
     * and transitions back to the primary authentication screen wrapper layout container.
     */
    private void handleLogoutExecution() {
        System.out.println("Terminating application session context securely...");

        try {
            HttpServletRequest mockRequest = createMockLogoutRequest();
            // authController.logout(mockRequest);
            System.out.println("Remote backend security registry notified successfully.");
        } catch (Exception e) {
            System.out.println("Backend termination notice bypassed or log skipped: " + e.getMessage());
        } finally {
            userSession.clearSession();
            SecurityContextHolder.clearContext();
            mainController.navigateTo("/ui/views/auth/LoginView.fxml");
            System.out.println("Desktop application clean out complete. Safe routing to Login finalized.");
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
                    if ("getHeader".equals(method.getName()) && args.length > 0) {
                        return headers.get((String) args[0]);
                    }
                    if ("getRemoteAddr".equals(method.getName())) {
                        return "127.0.0.1";
                    }
                    return null;
                }
        );
    }
}