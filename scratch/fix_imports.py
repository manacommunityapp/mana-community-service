import os
import re

service_dir = r"d:\Application\applications\mana community\mana-community-service"
src_java_dir = os.path.join(service_dir, "src", "main", "java")

imports_to_add = {
    r"\bAppUser\b": "import com.manacommunity.api.user.model.AppUser;",
    r"\bUserProfile\b": "import com.manacommunity.api.user.model.UserProfile;",
    r"\bUserSession\b": "import com.manacommunity.api.user.model.UserSession;",
    r"\bAppUserRepository\b": "import com.manacommunity.api.user.repository.AppUserRepository;",
    r"\bUserProfileRepository\b": "import com.manacommunity.api.user.repository.UserProfileRepository;",
    r"\bUserSessionRepository\b": "import com.manacommunity.api.user.repository.UserSessionRepository;",
    r"\bUserPrincipal\b": "import com.manacommunity.api.user.security.UserPrincipal;",
    r"\bSessionService\b": "import com.manacommunity.api.user.security.SessionService;",
    r"\bAuthService\b": "import com.manacommunity.api.user.service.AuthService;",
    r"\bUserProfileService\b": "import com.manacommunity.api.user.service.UserProfileService;",
    r"\bLoggedInUserService\b": "import com.manacommunity.api.user.service.LoggedInUserService;",
    r"\bLoginRequest\b": "import com.manacommunity.api.user.dto.LoginRequest;",
    r"\bRegisterRequest\b": "import com.manacommunity.api.user.dto.RegisterRequest;",
    r"\bRefreshTokenRequest\b": "import com.manacommunity.api.user.dto.RefreshTokenRequest;",
    r"\bKycRequest\b": "import com.manacommunity.api.user.dto.KycRequest;",
    r"\bUserProfileRequest\b": "import com.manacommunity.api.user.dto.UserProfileRequest;",
    r"\bUserProfileResponse\b": "import com.manacommunity.api.user.dto.UserProfileResponse;",
    r"\bUserResponse\b": "import com.manacommunity.api.user.dto.UserResponse;",
    r"\bAuthResponse\b": "import com.manacommunity.api.user.dto.AuthResponse;"
}

# Traverse all java files in src
for root, dirs, files in os.walk(src_java_dir):
    for file in files:
        if file.endswith(".java"):
            file_path = os.path.join(root, file)
            
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
                
            # Skip if it is under com.manacommunity.api.user, as they are already refactored/have correct packages
            rel_path = os.path.relpath(file_path, src_java_dir).replace("\\", "/")
            if rel_path.startswith("com/manacommunity/api/user/"):
                continue
                
            modified_content = content
            
            # Find package declaration line
            pkg_match = os.path.dirname(rel_path).replace("/", ".")
            
            # For each import we might need to add:
            for pattern, imp_stmt in imports_to_add.items():
                # If the word is present in the class, but the import statement itself is NOT present:
                import_regex = re.escape(imp_stmt)
                if re.search(pattern, content) and not re.search(import_regex, content):
                    # Also make sure the class is not defined in the same package (if package is e.g. com.manacommunity.api.user.model, it doesn't need to import AppUser)
                    # Deduce package of the import statement
                    imp_pkg = imp_stmt.split("import ")[1].rsplit(".", 1)[0]
                    if pkg_match == imp_pkg:
                        continue
                        
                    # Insert the import statement just after the package declaration
                    package_decl_regex = r"^(package\s+com\.manacommunity\.api\.[a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)*;)"
                    match = re.search(package_decl_regex, modified_content, re.MULTILINE)
                    if match:
                        package_line = match.group(1)
                        # Insert import with newline
                        modified_content = modified_content.replace(package_line, f"{package_line}\n\n{imp_stmt}")
                        
            if modified_content != content:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(modified_content)
                print(f"Added imports to: {rel_path}")

print("Import fixing completed.")
