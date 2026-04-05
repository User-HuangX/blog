INSERT INTO post_meta (id, title, author_name)
SELECT 1, 'Welcome to My Blog', 'Admin'
WHERE NOT EXISTS (SELECT 1 FROM post_meta WHERE id = 1);

INSERT INTO post_content (post_id, summary, content)
SELECT 1, 'First default post', 'This post is initialized by data.sql.'
WHERE NOT EXISTS (SELECT 1 FROM post_content WHERE post_id = 1);
