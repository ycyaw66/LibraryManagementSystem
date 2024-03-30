<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <!-- 标题 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>  

        <!-- 查询框第一行 -->
        <div style="margin-left: 20px; padding-top:3vh;">
            <el-input v-model="this.toQueryInfo.title" style="display:inline; margin-left: 20px;" placeholder="书名"></el-input>
            <el-input v-model="this.toQueryInfo.category" style="display:inline; margin-left: 20px;" placeholder="类别"></el-input>
            <el-input v-model="this.toQueryInfo.author" style="display:inline; margin-left: 20px;" placeholder="作者"></el-input>
            <el-input v-model="this.toQueryInfo.press" style="display:inline; margin-left: 20px;" placeholder="出版社"></el-input>
        </div>

        <!-- 查询框第二行 + 操作控件 -->
        <div style="margin-left: 20px; padding-top:1vh;">
            <el-input v-model="this.toQueryInfo.minPublishYear" style="display:inline; margin-left: 20px; margin-top: 20px;" placeholder="最小出版年份"></el-input> --
            <el-input v-model="this.toQueryInfo.maxPublishYear" style="display:inline; margin-left: 0; margin-top: 20px;" placeholder="最大出版年份"></el-input>
            <el-input v-model="this.toQueryInfo.minPrice" style="display:inline; margin-left: 20px; margin-top: 20px;" placeholder="最小价格"></el-input> --
            <el-input v-model="this.toQueryInfo.maxPrice" style="display:inline; margin-left: 0; margin-top: 20px;" placeholder="最大价格"></el-input>

            <el-button style="margin-left: 20px;" type="primary" @click="QueryBooks">查询</el-button>

            <el-button @click="newBookInfo.category = '', newBookInfo.title = '', newBookInfo.author = '', newBookInfo.press = '', newBookInfo.publishYear = 2000, newBookInfo.price = 0, newBookInfo.stock = 0, newBookVisible = true" style="margin-left: 20px;" type="success">新建图书</el-button>
        </div>

        <!-- 新建图书对话框 -->
        <el-dialog v-model="newBookVisible" title="新建图书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：
                <el-input v-model="newBookInfo.category" style="margin-left: 24pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="newBookInfo.title" style="margin-left: 24pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="newBookInfo.author" style="margin-left: 24pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="newBookInfo.press" style="margin-left: 12pt; width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input-number v-model="newBookInfo.publishYear" style="width: 12.5vw;" :min="-5000" :step="1" step-strictly />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input-number v-model="newBookInfo.price" style="margin-left: 24pt; width: 12.5vw;" :precision="2" :step="0.01" :min="0" />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                数量：
                <el-input-number v-model="newBookInfo.stock" style="margin-left: 24pt; width: 12.5vw;" :min="1" :step="1" step-strictly />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook" :disabled="newBookInfo.category.length === 0 || newBookInfo.title.length === 0 || newBookInfo.author.length === 0 || newBookInfo.press.length === 0 || newBookInfo.publishYear == null || newBookInfo.price == null || newBookInfo.stock == null">确定</el-button>
                </span>
            </template>
        </el-dialog>
        
        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="books" height="450"
            :default-sort="{ prop: 'bookID', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="bookID" label="编号" width="90" sortable align="center"/>
            <el-table-column prop="title" label="书名" width="200" sortable align="center"/>
            <el-table-column prop="author" label="作者" width="110" sortable align="center"/>
            <el-table-column prop="category" label="类别" width="150" sortable align="center"/>
            <el-table-column prop="press" label="出版社" width="110" sortable align="center"/>
            <el-table-column prop="publishYear" label="出版年份" width="110" sortable align="center"/>
            <el-table-column prop="price" label="价格" width="110" sortable :formatter="formatPrice" align="center"/>
            <el-table-column prop="stock" label="库存" width="110" sortable align="center"/>
            <el-table-column label="操作" align="center">
                <el-button size="small" type="primary" @click="handleEdit()">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete()">删除</el-button>
            </el-table-column>
        </el-table>
        
    </el-scrollbar>
</template>

<script>
import axios from 'axios';
import { ElMessage } from 'element-plus'

export default {
    data() {
        return {
            isShow: false, // 结果表格展示状态
            newBookVisible: false, // 新建图书对话框可见性
            books: [{ // 书籍列表
                bookID: 0,
                category: 'test',
                title: 'test',
                press: 'test',
                minPublishYear: 0,
                maxPublishYear: 0,
                author: 'test',
                minPrice: 0,
                maxPrice: 0,
                stock: 0
            }],
            newBookInfo: { // 待新建图书信息
                category: '',
                title: '',
                author: '',
                press: '',
                publishYear: 0,
                price: 0.0,
                stock: 0
            },
            toQueryInfo: { // 待查询图书信息
                category: '',
                title: '',
                press: '',
                minPublishYear: '',
                maxPublishYear: '',
                author: '',
                minPrice: '',
                maxPrice: ''
            },
        }
    },
    methods: {
        ConfirmNewBook() {
            axios.post("/book",
                {
                    category: this.newBookInfo.category,
                    title: this.newBookInfo.title,
                    author: this.newBookInfo.author,
                    press: this.newBookInfo.press,
                    publishYear: this.newBookInfo.publishYear,
                    price: this.newBookInfo.price,
                    stock: this.newBookInfo.stock,
                })
                .then(response => {
                    ElMessage.success(response.data)
                    this.newBookVisible = false
                })
                .catch(error => {
                    ElMessage.error(error.response.data)
                })
        },
        async QueryBooks() {
            this.books = [] // 清空列表
            let response = await axios.get('/book',
                {
                    params: {
                        category: this.toQueryInfo.category,
                        title: this.toQueryInfo.title,
                        press: this.toQueryInfo.press,
                        minPublishYear: this.toQueryInfo.minPublishYear,
                        maxPublishYear: this.toQueryInfo.maxPublishYear,
                        author: this.toQueryInfo.author,
                        minPrice: this.toQueryInfo.minPrice,
                        maxPrice: this.toQueryInfo.maxPrice
                    }
                })
            let books = response.data
            books.forEach(book => {
                this.books.push(book)
            });
            this.isShow = true
        },
        formatPrice(row, column, cellValue) {
            if (cellValue === null || cellValue === undefined) {
                return '';
            }
            return parseFloat(cellValue).toFixed(2);
        }
    }
}
</script>