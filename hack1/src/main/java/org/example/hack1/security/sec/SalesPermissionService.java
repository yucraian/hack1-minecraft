package security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SalesPermissionService {

    @Autowired
    private SecurityUtils securityUtils;

    public void validateBranchAccess(String branch) {
        if (!securityUtils.hasAccessToBranch(branch)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos para acceder a esta sucursal");
        }
    }

    public void validateSaleCreation(String requestedBranch) {
        if (!securityUtils.isCentralUser()) {
            String userBranch = securityUtils.getCurrentUserBranch();
            if (!userBranch.equals(requestedBranch)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo puedes crear ventas para tu sucursal: " + userBranch);
            }
        }
    }
}