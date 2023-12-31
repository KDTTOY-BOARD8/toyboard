package com.fastcampus.toyboard.user.service;

import com.fastcampus.toyboard.user.dto.BoardUserDto;
import com.fastcampus.toyboard.user.dto.BoardUserRequest;
import com.fastcampus.toyboard.user.model.BoardAuthority;
import com.fastcampus.toyboard.user.model.BoardUser;
import com.fastcampus.toyboard.user.repository.BoardAuthorityRepository;
import com.fastcampus.toyboard.user.repository.BoardUserRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardUserService implements UserDetailsService {

  private final BoardUserRepository boardUserRepository;
  private final BoardAuthorityRepository boardAuthorityRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return boardUserRepository
        .findBoardUserByUsername(username)
        .orElseThrow(
            () -> {
              throw new UsernameNotFoundException(username);
            });
  }

  public BoardUserDto signUp(BoardUserRequest.SignUpDto signUpDto) {
    BoardUserDto newUserDto = BoardUserDto.of(signUpDto);

    // 패스워드 암호화 후 유저 정보 저장
    newUserDto.setPassword(passwordEncoder.encode(newUserDto.getPassword()));
    BoardUser newBoardUser = boardUserRepository.save(BoardUser.of(newUserDto));

    // 새로 가입한 경우 새싹 유저 권한 부여
    BoardAuthority newRole = new BoardAuthority(newBoardUser.getUserId(), "SPROUT");
    boardAuthorityRepository.save(newRole);

    return BoardUserDto.fromEntity(
        boardUserRepository
            .findBoardUserByUsername(newBoardUser.getUsername())
            .orElseThrow(
                () -> {
                  throw new RuntimeException("Sign Up Error.");
                }));
  }

  private BoardUser save(BoardUser boardUser) {
    return boardUserRepository.save(boardUser);
  }

  public BoardUserDto findUser(String username) {
    return BoardUserDto.fromEntity(
        boardUserRepository
            .findBoardUserByUsername(username)
            .orElseThrow(
                () -> {
                  throw new UsernameNotFoundException("Can't find user : " + username);
                }));
  }

  public void addAuthority(Long userId, String authority) {
    boardUserRepository
        .findById(userId)
        .ifPresent(
            user -> {
              BoardAuthority newRole = new BoardAuthority(user.getUserId(), authority);
              Set<BoardAuthority> authorities = new HashSet<>();
              if (user.getAuthorities() == null) {
                authorities.add(newRole);
              } else if (!user.getAuthorities().contains(newRole)) {
                authorities.addAll(user.getAuthorities());
                authorities.add(newRole);
              }
              user.setAuthorities(authorities);
              save(user);
            });
  }

  public void removeAuthority(Long userId, String authority) {
    boardUserRepository
        .findById(userId)
        .ifPresent(
            user -> {
              if (user.getAuthorities() == null) {
                return;
              }

              BoardAuthority targetRole = new BoardAuthority(user.getUserId(), authority);

              if (user.getAuthorities().contains(targetRole)) {
                user.setAuthorities(
                    user.getAuthorities().stream()
                        .filter(auth -> !auth.equals(targetRole))
                        .collect(Collectors.toSet()));
                save(user);
              }
            });
  }

  // TODO: 2023/07/18 프론트에 중복체크 버튼 생성해야함, 연결은 마지막에

  @Transactional(readOnly = true)
  public Boolean usernameOverlap(String username) {
    return boardUserRepository.existsByUsername(username);
  }

  @Transactional(readOnly = true)
  public Boolean emailOverlap(String email) {
    return boardUserRepository.existsByEmail(email);
  }

  @Transactional(readOnly = true)
  public Boolean nicknameOverlap(String nickname) {
    return boardUserRepository.existsByNickname(nickname);
  }

  @Transactional
  public BoardUserDto editMyInfo(BoardUserRequest.EditInfoDto editDto, BoardUser boardUser) {
    System.out.println("testEdit : ");
    System.out.println("nickname : " + editDto.getNickname());
    System.out.println("email : " + editDto.getEmail());
    if (!editDto.getEmail().isEmpty()) {
      boardUser.updateEmail(editDto.getEmail());
    }

    if (!editDto.getNickname().isEmpty()) {
      boardUser.updateNickname(editDto.getNickname());
    }
    boardUserRepository.save(boardUser);
    return BoardUserDto.fromEntity(
        boardUserRepository
            .findBoardUserByUsername(boardUser.getUsername())
            .orElseThrow(
                () -> {
                  throw new RuntimeException("Update Error.");
                }));
  }

  public BoardUserDto editPassword(String currentPw, String toBePw, BoardUser boardUser) {
    if (!passwordEncoder.matches(currentPw, boardUser.getPassword())) {
      throw new IllegalArgumentException("Password Not Matches");
    }

    boardUser.setPassword(passwordEncoder.encode(toBePw));
    return BoardUserDto.fromEntity(boardUserRepository.save(boardUser));
  }
}
