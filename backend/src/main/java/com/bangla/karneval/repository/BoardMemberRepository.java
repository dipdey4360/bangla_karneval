package com.bangla.karneval.repository;
import com.bangla.karneval.model.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoardMemberRepository extends JpaRepository<BoardMember, Integer> {
    List<BoardMember> findAllByOrderByIdAsc();
}
