package com.fastcampus.toyboard.comment.model;

import com.fastcampus.toyboard.common.BaseTimeEntity;
import com.fastcampus.toyboard.user.domain.User;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "child_comment_tb")
@NoArgsConstructor
@Getter
public class ChildComment extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column private String content;

  @ManyToOne private Comment comment;

  @ManyToOne
  //  @JoinColumn(name="user_id")
  private User user;
}