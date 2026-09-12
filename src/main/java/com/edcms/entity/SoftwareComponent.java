package com.edcms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * A software or firmware component managed within the engineering system.
 *
 * <p>Examples: motor control firmware, battery management system (BMS) software,
 * embedded OS images, control algorithms, device drivers.
 *
 * <p><b>OOP — Inheritance:</b>
 * Extends {@link Component} with software-domain attributes: programming language
 * and license type. Software components participate in the same versioning,
 * assembly, and dependency management as physical components — treating them
 * as first-class components rather than a separate system.
 *
 * <p>In embedded engineering systems (like EV development), software is a
 * critical deliverable alongside hardware. The EDCMS models this correctly.
 */
@Entity
@Table(name = "software_components")
@DiscriminatorValue("SOFTWARE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SoftwareComponent extends Component {

    /**
     * Primary programming language used to develop this software component.
     * Example: "C", "C++", "Python", "Rust", "Java", "MATLAB/Simulink"
     */
    @Column(name = "programming_lang", length = 50)
    private String programmingLang;

    /**
     * Software license under which this component is distributed or used.
     * Example: "MIT", "Apache 2.0", "GPL-3.0", "Proprietary", "BSD-3-Clause"
     *
     * <p>License tracking is important for embedded systems to ensure
     * open-source license obligations are met before product release.
     */
    @Column(name = "license_type", length = 100)
    private String licenseType;
}
