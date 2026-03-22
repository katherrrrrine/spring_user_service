package org.example.service;

import org.example.dto.UserDto;
import org.example.exception.*;
import org.example.mapper.UserMapper;
import org.example.model.User;
import org.example.dao.UserRepository;
import org.example.validation.UserInputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto createUser(UserDto userDto) {
        UserInputValidator.validateUserDto(userDto);
        userDto.setEmail(userDto.getEmail().toLowerCase());
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserValidationException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }

        User user = UserMapper.toEntity(userDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserDto> findUserById(Long id) {
        if (id == null || id <= 0) {
            throw new UserValidationException("Неверный ID пользователя");
        }

        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(UserMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers() {
        try {
            return userRepository.findAll().stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new UserDaoException("Ошибка при получении списка пользователей", e);
        }
    }

    public UserDto updateUser(UserDto userDto) {
        if (userDto == null || userDto.getId() == null) {
            throw new UserValidationException("ID пользователя обязателен для обновления");
        }

        UserInputValidator.validateUserDto(userDto);
        userDto.setEmail(userDto.getEmail().toLowerCase());

        Optional<User> existingUserOpt = userRepository.findById(userDto.getId());
        if (existingUserOpt.isEmpty()) {
            throw new UserNotFoundException("Пользователь с ID " + userDto.getId() + " не найден");
        }

        User existingUser = existingUserOpt.get();

        if (!existingUser.getEmail().equals(userDto.getEmail()) &&
                userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserValidationException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }

        UserMapper.updateEntityFromDto(userDto, existingUser);

        try {
            User updatedUser = userRepository.save(existingUser);
            return UserMapper.toDto(updatedUser);
        } catch (Exception e) {
            throw new UserDaoException("Ошибка при обновлении пользователя с ID: " + userDto.getId(), e);
        }
    }

    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new UserValidationException("Неверный ID пользователя для удаления");
        }

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("Пользователь с ID " + id + " не найден");
        }

        try {
            userRepository.deleteById(id);
        } catch (Exception e) {
            throw new UserDaoException("Ошибка при удалении пользователя с ID: " + id, e);
        }
    }
}