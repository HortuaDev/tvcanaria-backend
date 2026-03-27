package com.tvcanaria.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tvcanaria.repository.UserRepository;

@Service
public class ReporterService {

    @Autowired
    private UserRepository userRepository;
}
