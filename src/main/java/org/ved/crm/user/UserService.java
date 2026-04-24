package org.ved.crm.user;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserDto> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDto getUserById(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));
        return userMapper.toDto(user);
    }

    @Transactional
    public void deactivateUser(UUID id){
        User user = userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("User","id",id));
        user.setActive(false);
        userRepository.save(user);
    }
}
