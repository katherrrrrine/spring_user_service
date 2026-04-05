package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Schema(description = "User Data Transfer Object")
public class UserDto {

    @Schema(description = "Unique user identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я\\s-]+$", message = "Name can only contain letters, spaces and hyphens")
    @Schema(description = "User's full name", example = "John Doe", required = true)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
    private String email;

    @NotNull(message = "Age is required")
    @Min(value = 9, message = "Age must be at least 9")
    @Max(value = 120, message = "Age must not exceed 120")
    @Schema(description = "User's age", example = "25", required = true, minimum = "9", maximum = "120")
    private Integer age;

    @Schema(description = "Account creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    public UserDto() {
    }

    public UserDto(Long id, String name, String email, Integer age, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("UserDto{id=%d, name='%s', email='%s', age=%d, createdAt=%s}",
                id, name, email, age, createdAt);
    }
}