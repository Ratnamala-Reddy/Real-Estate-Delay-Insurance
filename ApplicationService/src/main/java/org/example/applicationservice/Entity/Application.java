package org.example.applicationservice.Entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Table("applications")
public class Application {

    @Id
    @Column("application_id")
    private String applicationId;

    @Column("user_id")
    private Integer userId;

    @Column("builder_name")
    private String builderName;

    @Column("project_name")
    private String projectName;

    @Column("unit_number")
    private String unitNumber;

    @Column("property_value")
    private BigDecimal propertyValue;

    @Column("purchase_date")
    private LocalDate purchaseDate;

    @Column("promised_possession_date")
    private LocalDate promisedPossessionDate;

    @Column("status")
    private String status;
}