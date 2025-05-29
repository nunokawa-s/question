package com.example.NAGOYAMESHI.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class CategoryForm {
	 private Integer id;

	    @NotBlank(message = "カテゴリ名を入力してください。")
	    @Size(max = 50, message = "カテゴリ名は50文字以内で入力してください。")
	    private String name;
}
