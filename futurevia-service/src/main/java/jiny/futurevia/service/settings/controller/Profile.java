package jiny.futurevia.service.settings.controller;

import java.util.Optional;

import jiny.futurevia.service.account.domain.entity.Account;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    private String bio;
    private String url;
    private String job;
    private String location;

    public static Profile from(Account account) {
        return new Profile(account);
    }

    protected Profile(Account account) {
        this.bio = Optional.ofNullable(account.getProfile()).map(Account.Profile::getBio).orElse(null);
        this.url = Optional.ofNullable(account.getProfile()).map(Account.Profile::getUrl).orElse(null);
        this.job = Optional.ofNullable(account.getProfile()).map(Account.Profile::getJob).orElse(null);
        this.location = Optional.ofNullable(account.getProfile()).map(Account.Profile::getLocation).orElse(null);
    }
}