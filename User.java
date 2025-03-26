package com.viikko4;

public class User {
    private String username;
    private String password;
    private String email;

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() { 
        return username;
     }
    public String getPassword() { 
        return password; 
    }
    public String getEmail() { 
        
        return email;
     }

     @Override
     public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null || getClass() != obj.getClass()) return false;
         User user = (User) obj;
         return username.equals(user.username);
     }
 
     @Override
     public int hashCode() {
         return username.hashCode();
     }
 
}
