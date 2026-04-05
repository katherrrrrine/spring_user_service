package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.assembler.UserModelAssembler;
import org.example.dto.UserDto;
import org.example.exception.UserNotFoundException;
import org.example.model.UserModel;
import org.example.service.UserService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing users")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler assembler;

    public UserController(UserService userService, UserModelAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Create new user and returns the created user with HATEOAS links")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<UserModel> createUser(@Valid @RequestBody UserDto userDto) {
        UserDto createdUser = userService.createUser(userDto);
        UserModel userModel = assembler.toModel(createdUser);
        return new ResponseEntity<>(userModel, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Return user with HATEOAS links")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserModel.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserModel> getUserById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        UserDto userDto = userService.findUserById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + id + " не найден"));
        return ResponseEntity.ok(assembler.toModel(userDto));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Return list of all users with HATEOAS links")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list")
    public ResponseEntity<CollectionModel<UserModel>> getAllUsers() {
        List<UserDto> users = userService.findAllUsers();
        List<UserModel> userModels = users.stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        CollectionModel<UserModel> collectionModel = CollectionModel.of(userModels);
        collectionModel.add(linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel());
        collectionModel.add(linkTo(methodOn(UserController.class).createUser(null)).withRel("create"));

        return ResponseEntity.ok(collectionModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update existing user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserModel.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserModel> updateUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UserDto userDto) {
        userDto.setId(id);
        UserDto updatedUser = userService.updateUser(userDto);
        return ResponseEntity.ok(assembler.toModel(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Delete user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}