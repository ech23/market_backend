package com.example.ogani.controller;

import com.example.ogani.entity.Blog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.ogani.entity.User;
import com.example.ogani.model.request.ChangePasswordRequest;
import com.example.ogani.model.request.UpdateProfileRequest;
import com.example.ogani.model.response.MessageResponse;
import com.example.ogani.service.UserService;

import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/user")
//@CrossOrigin(origins = "*",maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;
    

    @GetMapping("/")
    @Operation(summary="Lấy ra user bằng username")
    public ResponseEntity<User> getuser(@RequestParam("username") String username){
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }
//    @GetMapping("/")
//    @Operation(summary="Lấy tất cả danh sách user")
//    public ResponseEntity<List<User>> getList(){
//        List<User> list = userService.getList();
//
//        return ResponseEntity.ok(list);
//
//    }
    @DeleteMapping("delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable long id){
        userService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("Delete uccess"));
    }
    @PutMapping("/update")
    @Operation(summary="Cập nhật user")
    public ResponseEntity<User> updateProfile(@RequestBody UpdateProfileRequest request){
        User user = userService.updateUser(request);

        return ResponseEntity.ok(user);
    }

     @PutMapping("/password")
     public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request){
         userService.changePassword(request);
         return ResponseEntity.ok(new MessageResponse("Change Password Success!"));
     }
}
