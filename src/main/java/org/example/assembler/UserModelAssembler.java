package org.example.assembler;

import org.example.controller.UserController;
import org.example.dto.UserDto;
import org.example.model.UserModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

@Component
public class UserModelAssembler extends RepresentationModelAssemblerSupport<UserDto, UserModel> {

    public UserModelAssembler() {
        super(UserController.class, UserModel.class);
    }

    @Override
    public UserModel toModel(UserDto userDto) {
        UserModel userModel = new UserModel(
                userDto.getId(),
                userDto.getName(),
                userDto.getEmail(),
                userDto.getAge(),
                userDto.getCreatedAt() != null ? userDto.getCreatedAt().toString() : null
        );

        userModel.add(linkTo(methodOn(UserController.class).getUserById(userDto.getId())).withSelfRel());

        userModel.add(linkTo(methodOn(UserController.class).updateUser(userDto.getId(), userDto)).withRel("update"));

        userModel.add(linkTo(methodOn(UserController.class).deleteUser(userDto.getId())).withRel("delete"));

        userModel.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));

        return userModel;
    }
}