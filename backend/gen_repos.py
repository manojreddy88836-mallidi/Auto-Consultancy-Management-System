import os

base_dir = r"c:\Users\LENOVO\OneDrive\Desktop\antigravity\Auto_Consultancy\backend\src\main\java\com\autoconsultancy"

repo_template = """package com.autoconsultancy.repository;

import com.autoconsultancy.entity.{entity};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

{custom_methods}
@Repository
public interface {entity}Repository extends JpaRepository<{entity}, {id_type}>, JpaSpecificationExecutor<{entity}> {
}
"""

repos = {
    "User": ("Long", "    java.util.Optional<User> findByEmail(String email);\n"),
    "Customer": ("Long", ""),
    "Worker": ("Long", ""),
    "Manufacturer": ("Long", "    java.util.List<Manufacturer> findByActiveTrue();\n"),
    "BikeModel": ("Long", "    java.util.List<BikeModel> findByManufacturerIdAndActiveTrue(Long manufacturerId);\n"),
    "BikeVariant": ("Long", "    java.util.List<BikeVariant> findByBikeModelIdAndActiveTrue(Long modelId);\n"),
    "ManufacturingYear": ("Long", "    java.util.List<ManufacturingYear> findByBikeModelIdAndActiveTrue(Long modelId);\n"),
    "Application": ("Long", "    java.util.List<Application> findByCustomerId(Long customerId);\n"),
    "BikeDetail": ("Long", ""),
    "FinanceDetail": ("Long", ""),
    "Document": ("Long", "    java.util.List<Document> findByApplicationId(Long applicationId);\n"),
    "ApplicationStatusHistory": ("Long", "    java.util.List<ApplicationStatusHistory> findByApplicationId(Long applicationId);\n"),
    "WorkerAssignment": ("Long", "    java.util.Optional<WorkerAssignment> findByApplicationId(Long applicationId);\n"),
    "Notification": ("Long", "    java.util.List<Notification> findByUserId(Long userId);\n"),
    "AuditLog": ("Long", "")
}

repo_dir = os.path.join(base_dir, "repository")
os.makedirs(repo_dir, exist_ok=True)

for entity, (id_type, custom) in repos.items():
    content = repo_template.replace("{entity}", entity).replace("{id_type}", id_type).replace("{custom_methods}", custom)
    # clean up optional custom methods
    if custom:
        content = content.replace("{\n}", "{\n" + custom + "}")
        content = content.replace("{custom_methods}", "")
    else:
        content = content.replace("{custom_methods}", "")
    
    with open(os.path.join(repo_dir, f"{entity}Repository.java"), "w") as f:
        f.write(content)

print("Generated repositories successfully.")
