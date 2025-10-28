
CREATE TABLE categories (
	category_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
	parent_category_id INT,
	category_title VARCHAR(255),
	image_url VARCHAR(255),
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	updated_at TIMESTAMP
);

