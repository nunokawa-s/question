package com.example.NAGOYAMESHI.form;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopEditForm {
	   private Integer id; // ★ これがあるか確認 ★
    @NotBlank(message = "店舗名を入力してください。")
    private String name;

    private MultipartFile imageFile;
    
    private List<Integer> categoryIds;

    @NotBlank(message = "説明を入力してください。")
    private String description;

    @NotNull(message = "価格を入力してください。")
    @Min(value = 0, message = "価格は0円以上に設定してください。")
    private Integer price;

    @NotNull(message = "定員を入力してください。")
    @Min(value = 1, message = "定員は1人以上に設定してください。")
    private Integer capacity;

    @NotBlank(message = "郵便番号を入力してください。")
    @Pattern(regexp = "^\\d{3}-?\\d{4}$", message = "郵便番号はXXX-XXXXの形式で入力してください。")
    private String postalCode;

    @NotBlank(message = "住所を入力してください。")
    private String address;

    @NotBlank(message = "電話番号を入力してください。")
    @Pattern(regexp = "^\\d{2,4}-?\\d{2,4}-?\\d{4}$|^\\d{10,11}$", message = "電話番号はXXX-XXXX-XXXXの形式で入力してください。")
    private String phoneNumber;

    private String mondayOpeningHours;
    private String mondayClosingHours;
    private Boolean mondayClosed = false; 
    private String tuesdayOpeningHours;
    private String tuesdayClosingHours;
    private Boolean tuesdayClosed = false; 
    private String wednesdayOpeningHours;
    private String wednesdayClosingHours;
    private Boolean wednesdayClosed = false; 
    private String thursdayOpeningHours;
    private String thursdayClosingHours;
    private Boolean thursdayClosed = false; 
    private String fridayOpeningHours;
    private String fridayClosingHours;
    private Boolean fridayClosed = false; 
    private String saturdayOpeningHours;
    private String saturdayClosingHours;
    private Boolean saturdayClosed = false; 
    private String sundayOpeningHours;
    private String sundayClosingHours;
    private Boolean sundayClosed = false; 
}