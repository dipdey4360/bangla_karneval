package com.bangla.karneval.service;

import com.bangla.karneval.model.Admin;
import com.bangla.karneval.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired private AdminRepository adminRepository;

    public List<Admin>     getAll()               { return adminRepository.findAll(); }
    public Optional<Admin> findByEmail(String e)  { return adminRepository.findByEmail(e); }
    public void            delete(Long id)        { adminRepository.deleteById(id); }
}
