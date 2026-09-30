ALTER TABLE questions
    ADD CONSTRAINT fk_questions_session
        FOREIGN KEY (session_id) REFERENCES exam_sessions (id);

ALTER TABLE answers
    ADD CONSTRAINT fk_answers_session
        FOREIGN KEY (session_id) REFERENCES exam_sessions (id);

ALTER TABLE answers
    ADD CONSTRAINT fk_answers_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id);

ALTER TABLE answers
    ADD CONSTRAINT fk_answers_question
        FOREIGN KEY (question_id) REFERENCES questions (id);

ALTER TABLE violations
    ADD CONSTRAINT fk_violations_session
        FOREIGN KEY (session_id) REFERENCES exam_sessions (id);

ALTER TABLE violations
    ADD CONSTRAINT fk_violations_user
        FOREIGN KEY (user_id) REFERENCES user_profiles (id);

ALTER TABLE votes
    ADD CONSTRAINT fk_votes_voting
        FOREIGN KEY (voting_id) REFERENCES secret_votings (id);

ALTER TABLE voting_options
    ADD CONSTRAINT fk_voting_options_candidate_user
        FOREIGN KEY (candidate_user_id) REFERENCES user_profiles (id);
