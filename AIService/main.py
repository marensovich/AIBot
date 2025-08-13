from openai import OpenAI

from fastapi import FastAPI
from pydantic import BaseModel


client = OpenAI(api_key="APIKEY", base_url="https://api.deepseek.com")


app = FastAPI()

class RequestData(BaseModel):
    text: str


@app.post("/ask_ai")
def ask_ai(request: RequestData):
    try:
        # Формируем сообщения в правильном формате для OpenAI
        messages = [
            {"role": "system", "content": "You are a helpful assistant."},
            {"role": "user", "content": request.text},
        ]

        # Отправляем запрос к DeepSeek API
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=messages,
            stream=False,
        )

        # Извлекаем ответ
        ai_response = response.choices[0].message.content
        return {"response": ai_response}

    except Exception as e:
        print(f"Ошибка: {e}")
        return {"error": str(e)}, 500


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9090)


