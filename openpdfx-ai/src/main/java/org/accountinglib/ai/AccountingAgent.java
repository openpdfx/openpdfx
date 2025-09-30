package org.accountinglib.ai;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;
import com.github.tjake.jlama.util.Downloader;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Test using JLama to run a local LLM model for accounting related questions.
 *
 * Must add VM options:
 * --enable-preview --add-modules jdk.incubator.vector
 */
public class AccountingAgent {

    public static String query(String prompt) throws IOException {
        //String model = "tjake/Llama-3.2-1B-Instruct-JQ4";
        String model = "tjake/Mistral-7B-Instruct-v0.3-JQ4";

        String workingDirectory = "./models";

        File localModelPath = new Downloader(workingDirectory, model).huggingFaceModel();

        AbstractModel m = ModelSupport.loadModel(localModelPath, DType.F32, DType.I8);

        PromptContext ctx;
        if (m.promptSupport().isPresent()) {
            ctx = m.promptSupport()
                    .get()
                    .builder()
                    .addSystemMessage("""
                                You are an accounting system based in Norway. 
                                Always return your response strictly in JSON format, without explanations or extra text. 
                                The JSON must represent a Voucher object with the following structure:
                        
                                {
                                  "id": <number>,
                                  "date": "YYYY-MM-DD",
                                  "postings": {
                                    "<postingId>": {
                                      "id": <number>,
                                      "account": { "id": <number>, "name": "<string>" },
                                      "date": "YYYY-MM-DD",
                                      "currency": { "code": "NOK" },
                                      "amount": <decimal>,
                                      "description": "<string>",
                                      "employee": null,
                                      "project": null
                                    }
                                  }
                                }
                        
                                Only output valid JSON. No prose, no markdown, no explanations.
                                """)
                    .addUserMessage(prompt)
                    .build();
        } else {
            ctx = PromptContext.of(prompt);
        }

        System.out.println("Prompt: " + ctx.getPrompt() + "\n");
        Generator.Response r = m.generate(UUID.randomUUID(), ctx, 0.0f, 2560, (s, f) -> {});
        System.out.println(r.responseText);
        return r.responseText;
    }


    public static void main(String[] args) {
        System.out.println("AccountingLib-AI test using JLama.");
        try {
            AccountingAgent.query("I sold milk for 1000 NOK. Make a voucher with debit posting and credit posting and VAT.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
